ALTER TABLE weekly_plans ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'VALIDATED';
ALTER TABLE weekly_plans ADD COLUMN source_weekly_plan_id BIGINT;
ALTER TABLE weekly_plans ADD COLUMN validated_at DATETIME(6);
ALTER TABLE weekly_plans ADD COLUMN validator_version VARCHAR(40) NOT NULL DEFAULT 'weekly-plan-validator-1.0';
ALTER TABLE weekly_plans ADD COLUMN reviewed_at DATETIME(6);
ALTER TABLE weekly_plans ADD COLUMN review_comment VARCHAR(1000);

UPDATE weekly_plans SET validated_at = created_at WHERE validated_at IS NULL;
ALTER TABLE weekly_plans MODIFY COLUMN validated_at DATETIME(6) NOT NULL;

ALTER TABLE weekly_plans ADD CONSTRAINT fk_weekly_plan_source
    FOREIGN KEY (source_weekly_plan_id) REFERENCES weekly_plans(id);

CREATE TABLE weekly_plan_validation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    weekly_plan_id BIGINT NOT NULL,
    severity VARCHAR(10) NOT NULL,
    code VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    CONSTRAINT uk_weekly_plan_validation_code UNIQUE (weekly_plan_id, code),
    CONSTRAINT fk_weekly_plan_validation FOREIGN KEY (weekly_plan_id)
        REFERENCES weekly_plans(id) ON DELETE CASCADE
);
