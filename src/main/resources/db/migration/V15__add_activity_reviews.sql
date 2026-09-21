CREATE TABLE activity_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    comparison_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    policy_version VARCHAR(50) NOT NULL,
    policy_reason VARCHAR(500) NOT NULL,
    model_called BOOLEAN NOT NULL,
    assessment VARCHAR(1500),
    model VARCHAR(100),
    response_id VARCHAR(100),
    input_tokens INT,
    output_tokens INT,
    latency_ms BIGINT,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    CONSTRAINT uk_activity_review_activity UNIQUE (activity_id),
    CONSTRAINT uk_activity_review_comparison UNIQUE (comparison_id),
    CONSTRAINT fk_activity_review_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_review_comparison FOREIGN KEY (comparison_id) REFERENCES activity_comparisons(id) ON DELETE CASCADE
);

CREATE TABLE activity_segment_detail_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    query_type VARCHAR(20) NOT NULL,
    resolution VARCHAR(20) NOT NULL,
    range_start DECIMAL(12,3) NOT NULL,
    range_end DECIMAL(12,3) NOT NULL,
    context_before DECIMAL(10,3) NOT NULL,
    context_after DECIMAL(10,3) NOT NULL,
    fields VARCHAR(200) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    points_returned INT NOT NULL,
    CONSTRAINT fk_activity_detail_review FOREIGN KEY (review_id) REFERENCES activity_reviews(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_detail_review_requested
    ON activity_segment_detail_requests (review_id, requested_at);
