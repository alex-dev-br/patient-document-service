CREATE TABLE access_audit_log (
    id UUID PRIMARY KEY,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100),
    patient_id UUID,
    actor_type VARCHAR(30) NOT NULL,
    actor_subject VARCHAR(512),
    actor_client_id VARCHAR(255),
    actor_username VARCHAR(255),
    outcome VARCHAR(20) NOT NULL,
    detail VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT ck_access_audit_actor_type
        CHECK (
            actor_type IN (
                'ANONYMOUS',
                'USER',
                'SERVICE_ACCOUNT'
            )
        ),

    CONSTRAINT ck_access_audit_outcome
        CHECK (
            outcome IN (
                'ALLOWED',
                'DENIED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_access_audit_patient_occurred_at
    ON access_audit_log (
        patient_id,
        occurred_at DESC
    );

CREATE INDEX idx_access_audit_actor_occurred_at
    ON access_audit_log (
        actor_subject,
        occurred_at DESC
    );

CREATE INDEX idx_access_audit_resource_occurred_at
    ON access_audit_log (
        resource_type,
        resource_id,
        occurred_at DESC
    );

CREATE INDEX idx_access_audit_action_outcome_occurred_at
    ON access_audit_log (
        action,
        outcome,
        occurred_at DESC
    );