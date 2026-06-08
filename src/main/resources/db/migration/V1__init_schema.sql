CREATE TABLE IF NOT EXISTS grading_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL,
    step VARCHAR(100) NOT NULL,
    message TEXT,
    level VARCHAR(10) NOT NULL DEFAULT 'INFO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_grading_logs_submission ON grading_logs(submission_id);
