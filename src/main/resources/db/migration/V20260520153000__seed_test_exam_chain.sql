-- =====================================================================
-- V20260520153000__seed_test_exam_chain.sql
-- Seed script for local manual testing and frontend UI integration
-- Chain: Tenant -> Instructor -> Exam -> ExamRule
-- =====================================================================

-- 1. Tenant
INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted, created_at, updated_at)
VALUES (
    '99999999-9999-9999-9999-999999999999'::uuid,
    'FPT University - Tech Lab',
    'fptu-techlab',
    'contact@fpt.edu.vn',
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (slug) DO NOTHING;

-- 2. Instructor User
INSERT INTO users (id, tenant_id, username, password, full_name, status, is_deleted, created_at, updated_at)
VALUES (
    '88888888-8888-8888-8888-888888888888'::uuid,
    '99999999-9999-9999-9999-999999999999'::uuid,
    'teacher.nam@fpt.edu.vn',
    '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm', -- Password@123
    'Phuong Nam',
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Assign INSTRUCTOR role to user
INSERT INTO user_roles (id, user_id, role_id)
VALUES (
    md5('teacher.nam@fpt.edu.vn:INSTRUCTOR')::uuid,
    '88888888-8888-8888-8888-888888888888'::uuid,
    md5('INSTRUCTOR')::uuid
)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3. Exam
INSERT INTO exams (id, tenant_id, title, markdown, time_limit_minutes, status, created_by_teacher_id, is_deleted, created_at, updated_at)
VALUES (
    '77777777-7777-7777-7777-777777777777'::uuid,
    '99999999-9999-9999-9999-999999999999'::uuid,
    'Final Practical Exam - Java Core & OOP',
    '# Java Practical Exam

Write a program to manage a Student Management System following OOP principles...',
    90,
    'PUBLISHED',
    '88888888-8888-8888-8888-888888888888'::uuid,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 4. ExamRule
INSERT INTO exam_rules (id, exam_id, rule_payload, playwright_zip_url, created_at, updated_at)
VALUES (
    '66666666-6666-6666-6666-666666666666'::uuid,
    '77777777-7777-7777-7777-777777777777'::uuid,
    '{"allowedKeywords": ["interface", "extends", "implements", "try", "catch"], "disallowedKeywords": ["System.exit", "Runtime.getRuntime"], "minClassesCount": 3}'::jsonb,
    '99999999-9999-9999-9999-999999999999/exams/77777777-7777-7777-7777-777777777777/grading_script.zip',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
