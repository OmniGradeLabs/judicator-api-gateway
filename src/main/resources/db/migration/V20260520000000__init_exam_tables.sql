-- =====================================================================
-- V20260520000000__init_exam_tables.sql
-- Exam & Grading domain — PostgreSQL schema
-- Module: com.judicator.gateway.modules.exam
-- =====================================================================
-- NOTE on FK strategy:
--   INTRA-MODULE  (exam → exam_rules, exam_submissions → submission_appeals):
--     Physical FOREIGN KEY constraints are added — safe, same bounded context.
--   CROSS-MODULE  (tenant_id, student_id, created_by_teacher_id, reviewed_by_teacher_id):
--     NO physical FK — preserves loose coupling between the exam and identity modules.
-- =====================================================================

CREATE TABLE exams
(
    id                      UUID                     NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE          DEFAULT CURRENT_TIMESTAMP,

    -- Cross-module logical reference — no FK to tenants table intentionally
    tenant_id               UUID                     NOT NULL,
    title                   VARCHAR(500)             NOT NULL,
    markdown                TEXT,
    time_limit_minutes      INTEGER,
    pace_limits             VARCHAR(255),
    status                  VARCHAR(20)              NOT NULL DEFAULT 'DRAFT',
    -- Cross-module logical reference — no FK to users table intentionally
    created_by_teacher_id   UUID                     NOT NULL,
    is_deleted              BOOLEAN                  NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_exams PRIMARY KEY (id),
    CONSTRAINT chk_exams_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED'))
);

CREATE TABLE exam_rules
(
    id                   UUID                     NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE          DEFAULT CURRENT_TIMESTAMP,

    -- Intra-module: physical FK is appropriate here
    exam_id              UUID                     NOT NULL,
    rule_payload         JSONB,
    playwright_zip_url   VARCHAR(1000),

    CONSTRAINT pk_exam_rules PRIMARY KEY (id),
    CONSTRAINT uk_exam_rules_exam UNIQUE (exam_id),
    CONSTRAINT fk_exam_rules_exam FOREIGN KEY (exam_id) REFERENCES exams (id) ON DELETE CASCADE
);

CREATE TABLE exam_submissions
(
    id                       UUID                     NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP WITH TIME ZONE          DEFAULT CURRENT_TIMESTAMP,

    -- Intra-module: physical FK is appropriate here
    exam_id                  UUID                     NOT NULL,
    -- Cross-module logical reference — no FK to users table intentionally
    student_id               UUID                     NOT NULL,
    source_file_url          VARCHAR(1000),
    auto_score               DOUBLE PRECISION,
    final_score              DOUBLE PRECISION,
    teacher_comment          TEXT,
    status                   VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    -- Cross-module logical reference — no FK to users table intentionally
    reviewed_by_teacher_id   UUID,
    submitted_at             TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_exam_submissions PRIMARY KEY (id),
    CONSTRAINT fk_es_exam FOREIGN KEY (exam_id) REFERENCES exams (id) ON DELETE CASCADE,
    CONSTRAINT chk_es_status CHECK (status IN ('PENDING', 'GRADED', 'VERIFIED'))
);

CREATE TABLE submission_appeals
(
    id                UUID                     NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE          DEFAULT CURRENT_TIMESTAMP,

    -- Intra-module: physical FK is appropriate here
    submission_id     UUID                     NOT NULL,
    -- Cross-module logical reference — no FK to users table intentionally
    student_id        UUID                     NOT NULL,
    complaint         TEXT,
    status            VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    teacher_response  TEXT,

    CONSTRAINT pk_submission_appeals PRIMARY KEY (id),
    CONSTRAINT fk_sa_submission FOREIGN KEY (submission_id) REFERENCES exam_submissions (id) ON DELETE CASCADE,
    CONSTRAINT chk_sa_status CHECK (status IN ('PENDING', 'RESOLVED'))
);

-- =====================================================================
-- Indexes
-- =====================================================================

-- Exams: tenant scoped queries are the most common access pattern
CREATE INDEX idx_exams_tenant_status ON exams (tenant_id, status, is_deleted);
CREATE INDEX idx_exams_teacher ON exams (created_by_teacher_id, is_deleted);

-- Submissions: look up all submissions for an exam, or all by a student
CREATE INDEX idx_es_exam_status ON exam_submissions (exam_id, status);
CREATE INDEX idx_es_student ON exam_submissions (student_id, status);

-- Appeals: look up pending appeals for a student or a submission
CREATE INDEX idx_sa_submission ON submission_appeals (submission_id, status);
CREATE INDEX idx_sa_student ON submission_appeals (student_id, status);

-- GIN index on JSONB column for efficient JSON querying
CREATE INDEX idx_exam_rules_payload ON exam_rules USING gin (rule_payload);
