package com.aicoach.backend.review;

import com.aicoach.backend.dto.ActivitySegmentDetailRequest;
import com.aicoach.backend.dto.ActivitySegmentDetails;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.ActivityRecordRepo;
import com.aicoach.backend.repository.ActivityReviewRepo;
import com.aicoach.backend.repository.ActivitySegmentDetailRequestRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivitySegmentDetailsService {
    private final ActivityReviewRepo reviewRepo;
    private final ActivitySegmentDetailRequestRepo requestRepo;
    private final ActivityRecordRepo recordRepo;
    private final Clock clock;

    @Transactional
    public ActivitySegmentDetails getActivitySegmentDetails(Long reviewId, ActivitySegmentDetailRequest request) {
        ActivityReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new SegmentDetailValidationException("Avaliação não encontrada: " + reviewId));
        if (review.getStatus() != ActivityReviewStatus.IN_PROGRESS) {
            throw new SegmentDetailValidationException("Detalhes só podem ser consultados durante uma avaliação em andamento");
        }
        Validated validated = validate(review, request);

        com.aicoach.backend.models.ActivitySegmentDetailRequest persisted =
                new com.aicoach.backend.models.ActivitySegmentDetailRequest();
        persisted.setReview(review);
        persisted.setQueryType(request.queryType());
        persisted.setResolution(request.resolution());
        persisted.setRangeStart(request.start());
        persisted.setRangeEnd(request.end());
        persisted.setContextBefore(validated.contextBefore());
        persisted.setContextAfter(validated.contextAfter());
        persisted.setFields(String.join(",", validated.fields().stream().map(Enum::name).toList()));
        persisted.setReason(request.reason().trim());
        persisted.setRequestedAt(clock.instant());
        persisted.setPointsReturned(0);
        requestRepo.saveAndFlush(persisted);
        review.getDetailRequests().add(persisted);

        ActivitySegmentDetails details = request.resolution() == SegmentResolution.STEP
                ? stepDetails(review, request, validated)
                : telemetryDetails(review, request, validated);
        persisted.setPointsReturned(details.steps().size() + details.points().size());
        requestRepo.save(persisted);
        return details;
    }

    private Validated validate(ActivityReview review, ActivitySegmentDetailRequest request) {
        if (request == null || request.queryType() == null || request.resolution() == null
                || request.start() == null || request.end() == null) {
            throw new SegmentDetailValidationException("Tipo, intervalo e resolução são obrigatórios");
        }
        if (request.start().signum() < 0 || request.end().compareTo(request.start()) <= 0) {
            throw new SegmentDetailValidationException("O intervalo deve ser positivo e terminar após o início");
        }
        if (request.reason() == null || request.reason().trim().length() < 12) {
            throw new SegmentDetailValidationException("A inspeção exige uma justificativa específica com ao menos 12 caracteres");
        }
        List<TelemetryField> fields = request.fields() == null ? List.of() : request.fields().stream()
                .filter(Objects::nonNull).distinct().toList();
        if (fields.isEmpty()) {
            throw new SegmentDetailValidationException("Selecione ao menos um campo de telemetria");
        }
        BigDecimal defaultContext = request.queryType() == SegmentQueryType.TIME
                ? BigDecimal.valueOf(30) : new BigDecimal("0.100");
        BigDecimal maxContext = request.queryType() == SegmentQueryType.TIME
                ? BigDecimal.valueOf(60) : new BigDecimal("0.250");
        BigDecimal before = request.contextBefore() == null ? defaultContext : request.contextBefore();
        BigDecimal after = request.contextAfter() == null ? defaultContext : request.contextAfter();
        if (before.signum() < 0 || after.signum() < 0
                || before.compareTo(maxContext) > 0 || after.compareTo(maxContext) > 0) {
            throw new SegmentDetailValidationException("Contexto excede o limite de 60 s ou 0,25 km");
        }
        validateWindow(request);
        validateProgression(review, request);
        return new Validated(fields, before, after);
    }

    private void validateWindow(ActivitySegmentDetailRequest request) {
        BigDecimal size = request.end().subtract(request.start());
        BigDecimal max = switch (request.resolution()) {
            case STEP -> request.queryType() == SegmentQueryType.TIME ? BigDecimal.valueOf(21600) : BigDecimal.valueOf(100);
            case TEN_SECONDS -> request.queryType() == SegmentQueryType.TIME ? BigDecimal.valueOf(1800) : BigDecimal.valueOf(5);
            case FIVE_SECONDS -> request.queryType() == SegmentQueryType.TIME ? BigDecimal.valueOf(600) : BigDecimal.valueOf(2);
            case RAW -> request.queryType() == SegmentQueryType.TIME ? BigDecimal.valueOf(120) : new BigDecimal("0.500");
        };
        if (size.compareTo(max) > 0) {
            throw new SegmentDetailValidationException("Intervalo amplo demais para a resolução " + request.resolution());
        }
    }

    private void validateProgression(ActivityReview review, ActivitySegmentDetailRequest request) {
        SegmentResolution prerequisite = switch (request.resolution()) {
            case STEP, TEN_SECONDS -> null;
            case FIVE_SECONDS -> SegmentResolution.TEN_SECONDS;
            case RAW -> SegmentResolution.FIVE_SECONDS;
        };
        if (prerequisite == null) return;
        boolean found = requestRepo.findByReviewIdOrderByRequestedAtAsc(review.getId()).stream()
                .anyMatch(previous -> previous.getResolution() == prerequisite
                        && previous.getQueryType() == request.queryType()
                        && previous.getRangeStart().compareTo(request.start()) <= 0
                        && previous.getRangeEnd().compareTo(request.end()) >= 0);
        if (!found) {
            throw new SegmentDetailValidationException(request.resolution()
                    + " exige inspeção anterior sobreposta em " + prerequisite);
        }
    }

    private ActivitySegmentDetails stepDetails(ActivityReview review, ActivitySegmentDetailRequest request,
                                               Validated validated) {
        List<ActivitySegmentDetails.Step> steps = review.getComparison().getSteps().stream()
                .filter(step -> overlaps(step, request))
                .map(step -> new ActivitySegmentDetails.Step(step.getSequenceNumber(),
                        step.getIntervalStartSeconds(), step.getIntervalEndSeconds(),
                        step.getIntervalStartMeters(), step.getIntervalEndMeters(),
                        step.getClassification().name(), step.getCompliancePercentage(),
                        selected(validated, TelemetryField.PACE, step.getActualPaceSecondsPerKm()),
                        selected(validated, TelemetryField.HEART_RATE, step.getActualAverageHeartRate()),
                        selected(validated, TelemetryField.CADENCE, step.getActualAverageCadence()),
                        step.getExplanation())).toList();
        BigDecimal lower = lowerBound(request, validated);
        BigDecimal upper = upperBound(request, validated);
        return new ActivitySegmentDetails(review.getActivity().getId(), request.queryType(), request.start(),
                request.end(), lower, upper, request.resolution(), validated.fields(), request.reason(), steps, List.of());
    }

    private ActivitySegmentDetails telemetryDetails(ActivityReview review, ActivitySegmentDetailRequest request,
                                                    Validated validated) {
        BigDecimal lower = lowerBound(request, validated);
        BigDecimal upper = upperBound(request, validated);
        List<ActivityRecord> selectedRecords = recordRepo.findForActivityOrdered(review.getActivity().getId())
                .stream().filter(record -> within(record, request.queryType(), lower, upper)).toList();
        List<ActivitySegmentDetails.Point> points = request.resolution() == SegmentResolution.RAW
                ? selectedRecords.stream().map(record -> point(List.of(record), validated)).toList()
                : aggregate(selectedRecords, request.resolution(), validated);
        return new ActivitySegmentDetails(review.getActivity().getId(), request.queryType(), request.start(),
                request.end(), lower, upper, request.resolution(), validated.fields(), request.reason(), List.of(), points);
    }

    private List<ActivitySegmentDetails.Point> aggregate(List<ActivityRecord> records, SegmentResolution resolution,
                                                         Validated validated) {
        int bucketSeconds = resolution == SegmentResolution.TEN_SECONDS ? 10 : 5;
        Map<Integer, List<ActivityRecord>> buckets = new LinkedHashMap<>();
        for (ActivityRecord record : records) {
            if (record.getElapsedS() == null) continue;
            int bucket = Math.floorDiv(record.getElapsedS(), bucketSeconds);
            buckets.computeIfAbsent(bucket, ignored -> new ArrayList<>()).add(record);
        }
        return buckets.values().stream().map(bucket -> point(bucket, validated)).toList();
    }

    private ActivitySegmentDetails.Point point(List<ActivityRecord> records, Validated validated) {
        ActivityRecord first = records.get(0);
        return new ActivitySegmentDetails.Point(first.getId() == null ? null : first.getId().getTs(),
                average(records, ActivityRecord::getElapsedS), average(records, ActivityRecord::getDistanceKm),
                selected(validated, TelemetryField.PACE, average(records, ActivityRecord::getPaceSPerKm)),
                selected(validated, TelemetryField.HEART_RATE, average(records, ActivityRecord::getHeartRate)),
                selected(validated, TelemetryField.CADENCE, average(records, ActivityRecord::getCadence)),
                selected(validated, TelemetryField.ALTITUDE, average(records, ActivityRecord::getAltitudeM)));
    }

    private BigDecimal average(List<ActivityRecord> records, java.util.function.Function<ActivityRecord, Number> getter) {
        double sum = 0;
        int count = 0;
        for (ActivityRecord record : records) {
            Number number = getter.apply(record);
            if (number != null) { sum += number.doubleValue(); count++; }
        }
        return count == 0 ? null : BigDecimal.valueOf(sum / count).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal selected(Validated validated, TelemetryField field, BigDecimal value) {
        return validated.fields().contains(field) ? value : null;
    }

    private boolean within(ActivityRecord record, SegmentQueryType type, BigDecimal lower, BigDecimal upper) {
        BigDecimal value = type == SegmentQueryType.TIME
                ? decimal(record.getElapsedS()) : record.getDistanceKm();
        return value != null && value.compareTo(lower) >= 0 && value.compareTo(upper) <= 0;
    }

    private boolean overlaps(ActivityStepComparison step, ActivitySegmentDetailRequest request) {
        BigDecimal start = request.queryType() == SegmentQueryType.TIME
                ? step.getIntervalStartSeconds() : metersToKm(step.getIntervalStartMeters());
        BigDecimal end = request.queryType() == SegmentQueryType.TIME
                ? step.getIntervalEndSeconds() : metersToKm(step.getIntervalEndMeters());
        return start != null && end != null && end.compareTo(request.start()) >= 0
                && start.compareTo(request.end()) <= 0;
    }

    private BigDecimal lowerBound(ActivitySegmentDetailRequest request, Validated validated) {
        return request.start().subtract(validated.contextBefore()).max(BigDecimal.ZERO);
    }

    private BigDecimal upperBound(ActivitySegmentDetailRequest request, Validated validated) {
        return request.end().add(validated.contextAfter());
    }

    private BigDecimal metersToKm(BigDecimal meters) {
        return meters == null ? null : meters.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(Number value) {
        return value == null ? null : BigDecimal.valueOf(value.doubleValue());
    }

    private record Validated(List<TelemetryField> fields, BigDecimal contextBefore, BigDecimal contextAfter) {}
}
