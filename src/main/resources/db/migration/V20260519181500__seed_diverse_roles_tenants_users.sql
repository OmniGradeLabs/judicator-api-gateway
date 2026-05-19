-- Seed diverse roles, permissions, tenants, and test users for UI testing.
-- Default password for ALL seeded users: Password@123
-- BCrypt hash: $2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm

-- ============================================================
-- Tenant-level Permissions (scope: 'Quyền trong Tenant')
-- Organised by domain module for the Judicator grading platform.
-- ============================================================
INSERT INTO permissions (id, name, scope, module, resource, label, action)
VALUES
    -- ── QUAN_LY_THI (Exam Management) ──────────────────────────────────────
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.CREATE')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.CREATE',
        'Quyền trong Tenant', 'QUAN_LY_THI', 'Exam', 'Bài thi', 'CREATE'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.VIEW')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.VIEW',
        'Quyền trong Tenant', 'QUAN_LY_THI', 'Exam', 'Bài thi', 'VIEW'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.UPDATE')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.UPDATE',
        'Quyền trong Tenant', 'QUAN_LY_THI', 'Exam', 'Bài thi', 'UPDATE'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.DELETE')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.DELETE',
        'Quyền trong Tenant', 'QUAN_LY_THI', 'Exam', 'Bài thi', 'DELETE'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.PUBLISH')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.PUBLISH',
        'Quyền trong Tenant', 'QUAN_LY_THI', 'Exam', 'Bài thi', 'PUBLISH'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.TAKE')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.TAKE',
        'Quyền trong Tenant', 'QUAN_LY_THI', 'Exam', 'Bài thi', 'TAKE'
    ),

    -- ── QUAN_LY_BAI_NOP (Submission Management) ────────────────────────────
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.VIEW')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.VIEW',
        'Quyền trong Tenant', 'QUAN_LY_BAI_NOP', 'Submission', 'Bài nộp', 'VIEW'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.GRADE')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.GRADE',
        'Quyền trong Tenant', 'QUAN_LY_BAI_NOP', 'Submission', 'Bài nộp', 'GRADE'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.EXPORT')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.EXPORT',
        'Quyền trong Tenant', 'QUAN_LY_BAI_NOP', 'Submission', 'Bài nộp', 'EXPORT'
    ),

    -- ── QUAN_LY_NGUOI_DUNG (User Management within Tenant) ─────────────────
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_NGUOI_DUNG.USER.CREATE')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_NGUOI_DUNG.USER.CREATE',
        'Quyền trong Tenant', 'QUAN_LY_NGUOI_DUNG', 'User', 'Người dùng', 'CREATE'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_NGUOI_DUNG.USER.VIEW')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_NGUOI_DUNG.USER.VIEW',
        'Quyền trong Tenant', 'QUAN_LY_NGUOI_DUNG', 'User', 'Người dùng', 'VIEW'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_NGUOI_DUNG.USER.UPDATE')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_NGUOI_DUNG.USER.UPDATE',
        'Quyền trong Tenant', 'QUAN_LY_NGUOI_DUNG', 'User', 'Người dùng', 'UPDATE'
    ),
    (
        md5('QUYEN_TREN_TENANT.QUAN_LY_NGUOI_DUNG.USER.DELETE')::uuid,
        'QUYEN_TREN_TENANT.QUAN_LY_NGUOI_DUNG.USER.DELETE',
        'Quyền trong Tenant', 'QUAN_LY_NGUOI_DUNG', 'User', 'Người dùng', 'DELETE'
    ),

    -- ── BAO_CAO (Report) ────────────────────────────────────────────────────
    (
        md5('QUYEN_TREN_TENANT.BAO_CAO.REPORT.VIEW')::uuid,
        'QUYEN_TREN_TENANT.BAO_CAO.REPORT.VIEW',
        'Quyền trong Tenant', 'BAO_CAO', 'Report', 'Báo cáo', 'VIEW'
    ),
    (
        md5('QUYEN_TREN_TENANT.BAO_CAO.REPORT.EXPORT')::uuid,
        'QUYEN_TREN_TENANT.BAO_CAO.REPORT.EXPORT',
        'Quyền trong Tenant', 'BAO_CAO', 'Report', 'Báo cáo', 'EXPORT'
    ),

    -- ── KET_QUA (Personal Result — Student only) ───────────────────────────
    (
        md5('QUYEN_TREN_TENANT.KET_QUA.RESULT.VIEW_OWN')::uuid,
        'QUYEN_TREN_TENANT.KET_QUA.RESULT.VIEW_OWN',
        'Quyền trong Tenant', 'KET_QUA', 'Result', 'Kết quả cá nhân', 'VIEW_OWN'
    )
ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- 3 new Roles (SYSTEM_ADMIN is seeded in the previous migration)
-- ============================================================
INSERT INTO roles (id, name, "desc")
VALUES
    (
        md5('TENANT_ADMIN')::uuid,
        'TENANT_ADMIN',
        'Quản trị viên Tenant — quản lý users, phân quyền, xem báo cáo trong phạm vi trường/tổ chức'
    ),
    (
        md5('INSTRUCTOR')::uuid,
        'INSTRUCTOR',
        'Giảng viên — tạo và quản lý bài thi, chấm điểm bài nộp của sinh viên'
    ),
    (
        md5('STUDENT')::uuid,
        'STUDENT',
        'Sinh viên — tham gia thi và xem kết quả cá nhân'
    )
ON CONFLICT (name) DO NOTHING;

-- ── TENANT_ADMIN: all tenant-scope permissions ──────────────────────────────
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT md5('TENANT_ADMIN:' || p.name)::uuid, r.id, p.id
FROM   roles r
CROSS JOIN permissions p
WHERE  r.name = 'TENANT_ADMIN'
  AND  p.scope = 'Quyền trong Tenant'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── INSTRUCTOR: exam + submission + report (view) ───────────────────────────
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT md5('INSTRUCTOR:' || p.name)::uuid, r.id, p.id
FROM   roles r, permissions p
WHERE  r.name = 'INSTRUCTOR'
  AND  p.name IN (
      'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.CREATE',
      'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.VIEW',
      'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.UPDATE',
      'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.DELETE',
      'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.PUBLISH',
      'QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.VIEW',
      'QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.GRADE',
      'QUYEN_TREN_TENANT.QUAN_LY_BAI_NOP.SUBMISSION.EXPORT',
      'QUYEN_TREN_TENANT.BAO_CAO.REPORT.VIEW'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── STUDENT: view exams, take exams, view own results ───────────────────────
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT md5('STUDENT:' || p.name)::uuid, r.id, p.id
FROM   roles r, permissions p
WHERE  r.name = 'STUDENT'
  AND  p.name IN (
      'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.VIEW',
      'QUYEN_TREN_TENANT.QUAN_LY_THI.EXAM.TAKE',
      'QUYEN_TREN_TENANT.KET_QUA.RESULT.VIEW_OWN'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- Tenant 1: Đại học Bách Khoa TP.HCM
-- ============================================================
INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted)
VALUES (
    md5('dai-hoc-bach-khoa-hcm')::uuid,
    'Đại học Bách Khoa TP.HCM',
    'dai-hoc-bach-khoa-hcm',
    'contact@hcmut.edu.vn',
    'ACTIVE',
    false
)
ON CONFLICT (slug) DO NOTHING;

-- ── Users for Tenant 1 ──────────────────────────────────────────────────────
INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
VALUES
    -- Tenant Admin
    (
        md5('admin@hcmut.edu.vn')::uuid,
        md5('dai-hoc-bach-khoa-hcm')::uuid,
        'admin@hcmut.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Nguyễn Quản Trị',
        '0901001001',
        'ACTIVE', NULL, false
    ),
    -- Instructor
    (
        md5('gv.tran@hcmut.edu.vn')::uuid,
        md5('dai-hoc-bach-khoa-hcm')::uuid,
        'gv.tran@hcmut.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Trần Văn Giảng',
        '0901001002',
        'ACTIVE', NULL, false
    ),
    -- Student
    (
        md5('sv.le@hcmut.edu.vn')::uuid,
        md5('dai-hoc-bach-khoa-hcm')::uuid,
        'sv.le@hcmut.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Lê Văn Sinh',
        '0901001003',
        'ACTIVE', NULL, false
    )
ON CONFLICT (username) DO NOTHING;

-- ── Role assignments for Tenant 1 users ─────────────────────────────────────
INSERT INTO user_roles (id, user_id, role_id)
VALUES
    (
        md5('admin@hcmut.edu.vn:TENANT_ADMIN')::uuid,
        md5('admin@hcmut.edu.vn')::uuid,
        md5('TENANT_ADMIN')::uuid
    ),
    (
        md5('gv.tran@hcmut.edu.vn:INSTRUCTOR')::uuid,
        md5('gv.tran@hcmut.edu.vn')::uuid,
        md5('INSTRUCTOR')::uuid
    ),
    (
        md5('sv.le@hcmut.edu.vn:STUDENT')::uuid,
        md5('sv.le@hcmut.edu.vn')::uuid,
        md5('STUDENT')::uuid
    )
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ============================================================
-- Tenant 2: THPT Chuyên Lê Hồng Phong
-- ============================================================
INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted)
VALUES (
    md5('thpt-le-hong-phong')::uuid,
    'THPT Chuyên Lê Hồng Phong',
    'thpt-le-hong-phong',
    'contact@lehongphong.edu.vn',
    'ACTIVE',
    false
)
ON CONFLICT (slug) DO NOTHING;

-- ── Users for Tenant 2 ──────────────────────────────────────────────────────
INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
VALUES
    -- Tenant Admin
    (
        md5('admin@lehongphong.edu.vn')::uuid,
        md5('thpt-le-hong-phong')::uuid,
        'admin@lehongphong.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Phạm Quản Lý',
        '0902002001',
        'ACTIVE', NULL, false
    ),
    -- Instructor
    (
        md5('gv.hoang@lehongphong.edu.vn')::uuid,
        md5('thpt-le-hong-phong')::uuid,
        'gv.hoang@lehongphong.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Hoàng Thị Dạy',
        '0902002002',
        'ACTIVE', NULL, false
    ),
    -- Student
    (
        md5('sv.nguyen@lehongphong.edu.vn')::uuid,
        md5('thpt-le-hong-phong')::uuid,
        'sv.nguyen@lehongphong.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Nguyễn Thị Học',
        '0902002003',
        'ACTIVE', NULL, false
    )
ON CONFLICT (username) DO NOTHING;

-- ── Role assignments for Tenant 2 users ─────────────────────────────────────
INSERT INTO user_roles (id, user_id, role_id)
VALUES
    (
        md5('admin@lehongphong.edu.vn:TENANT_ADMIN')::uuid,
        md5('admin@lehongphong.edu.vn')::uuid,
        md5('TENANT_ADMIN')::uuid
    ),
    (
        md5('gv.hoang@lehongphong.edu.vn:INSTRUCTOR')::uuid,
        md5('gv.hoang@lehongphong.edu.vn')::uuid,
        md5('INSTRUCTOR')::uuid
    ),
    (
        md5('sv.nguyen@lehongphong.edu.vn:STUDENT')::uuid,
        md5('sv.nguyen@lehongphong.edu.vn')::uuid,
        md5('STUDENT')::uuid
    )
ON CONFLICT (user_id, role_id) DO NOTHING;
