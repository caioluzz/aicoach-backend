package com.aicoach.backend.training.daniels;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DanielsPerformanceEngineTest {

    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    private final DanielsPerformanceEngine engine = new DanielsPerformanceEngine(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void reproducesVdot50ReferenceForThreeKilometres() {
        PerformanceProfile profile = engine.fromThreeKmTest(3_000, 11 * 60 + 33);

        assertThat(profile.vdot()).isEqualTo(49.98);
        assertThat(profile.calculatedAt()).isEqualTo(NOW);
        assertThat(profile.engineVersion()).isEqualTo("daniels-2ed-ptbr-v1");
        assertThat(profile.coefficientSet()).isEqualTo("daniels-gilbert-vdot-2ed");
        assertThat(profile.paces()).containsOnlyKeys(DanielsIntensity.values());
        assertThat(profile.paces().get(DanielsIntensity.E)).isEqualTo(new PaceRange(294, 352));
        assertThat(profile.paces().get(DanielsIntensity.M)).isEqualTo(new PaceRange(265, 290));
        assertThat(profile.paces().get(DanielsIntensity.T)).isEqualTo(new PaceRange(255, 268));
        assertThat(profile.paces().get(DanielsIntensity.I)).isEqualTo(new PaceRange(230, 240));
        assertThat(profile.paces().get(DanielsIntensity.R)).isEqualTo(new PaceRange(215, 225));
    }

    @Test
    void acceptsSmallGpsVariationButNormalizesCalculationToThreeKilometres() {
        PerformanceProfile shortCourse = engine.fromThreeKmTest(2_970, 12 * 60);
        PerformanceProfile exactCourse = engine.fromThreeKmTest(3_000, 12 * 60);

        assertThat(shortCourse.vdot()).isEqualTo(exactCourse.vdot());
        assertThat(shortCourse.testDistanceMeters()).isEqualTo(2_970);
    }

    @Test
    void rejectsTestOutsideExplicitDistanceTolerance() {
        assertThatThrownBy(() -> engine.fromThreeKmTest(2_969, 12 * 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2970 and 3030");
    }
}
