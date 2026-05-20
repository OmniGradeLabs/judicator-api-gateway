-- ============================================================
-- Tenant 3: Đại học FPT
-- ============================================================
INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted)
VALUES (
    md5('dai-hoc-fpt')::uuid,
    'Đại học FPT',
    'dai-hoc-fpt',
    'contact@fpt.edu.vn',
    'ACTIVE',
    false
)
ON CONFLICT (slug) DO NOTHING;

-- ── Users for Tenant 3 ──────────────────────────────────────────────────────
INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
VALUES
    -- Tenant Admin
    (
        md5('admin@fpt.edu.vn')::uuid,
        md5('dai-hoc-fpt')::uuid,
        'admin@fpt.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'FPT Admin',
        '0903003001',
        'ACTIVE', NULL, false
    ),
    -- Instructor
    (
        md5('gv.nam@fpt.edu.vn')::uuid,
        md5('dai-hoc-fpt')::uuid,
        'gv.nam@fpt.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Nam Giảng Viên',
        '0903003002',
        'ACTIVE', NULL, false
    ),
    -- Student
    (
        md5('sv.binh@fpt.edu.vn')::uuid,
        md5('dai-hoc-fpt')::uuid,
        'sv.binh@fpt.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Bình Sinh Viên',
        '0903003003',
        'ACTIVE', NULL, false
    )
ON CONFLICT (username) DO NOTHING;

-- ── Role assignments for Tenant 3 users ─────────────────────────────────────
INSERT INTO user_roles (id, user_id, role_id)
VALUES
    (
        md5('admin@fpt.edu.vn:TENANT_ADMIN')::uuid,
        md5('admin@fpt.edu.vn')::uuid,
        md5('TENANT_ADMIN')::uuid
    ),
    (
        md5('gv.nam@fpt.edu.vn:INSTRUCTOR')::uuid,
        md5('gv.nam@fpt.edu.vn')::uuid,
        md5('INSTRUCTOR')::uuid
    ),
    (
        md5('sv.binh@fpt.edu.vn:STUDENT')::uuid,
        md5('sv.binh@fpt.edu.vn')::uuid,
        md5('STUDENT')::uuid
    )
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ============================================================
-- Tenant 4: Đại học Quốc gia Hà Nội
-- ============================================================
INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted)
VALUES (
    md5('vnu-hanoi')::uuid,
    'Đại học Quốc gia Hà Nội',
    'vnu-hanoi',
    'contact@vnu.edu.vn',
    'ACTIVE',
    false
)
ON CONFLICT (slug) DO NOTHING;

-- ── Users for Tenant 4 ──────────────────────────────────────────────────────
INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
VALUES
    -- Tenant Admin
    (
        md5('admin@vnu.edu.vn')::uuid,
        md5('vnu-hanoi')::uuid,
        'admin@vnu.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'VNU Admin',
        '0904004001',
        'ACTIVE', NULL, false
    ),
    -- Instructor
    (
        md5('gv.huong@vnu.edu.vn')::uuid,
        md5('vnu-hanoi')::uuid,
        'gv.huong@vnu.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Hương Giảng Viên',
        '0904004002',
        'ACTIVE', NULL, false
    ),
    -- Student
    (
        md5('sv.duc@vnu.edu.vn')::uuid,
        md5('vnu-hanoi')::uuid,
        'sv.duc@vnu.edu.vn',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Đức Sinh Viên',
        '0904004003',
        'ACTIVE', NULL, false
    )
ON CONFLICT (username) DO NOTHING;

-- ── Role assignments for Tenant 4 users ─────────────────────────────────────
INSERT INTO user_roles (id, user_id, role_id)
VALUES
    (
        md5('admin@vnu.edu.vn:TENANT_ADMIN')::uuid,
        md5('admin@vnu.edu.vn')::uuid,
        md5('TENANT_ADMIN')::uuid
    ),
    (
        md5('gv.huong@vnu.edu.vn:INSTRUCTOR')::uuid,
        md5('gv.huong@vnu.edu.vn')::uuid,
        md5('INSTRUCTOR')::uuid
    ),
    (
        md5('sv.duc@vnu.edu.vn:STUDENT')::uuid,
        md5('sv.duc@vnu.edu.vn')::uuid,
        md5('STUDENT')::uuid
    )
ON CONFLICT (user_id, role_id) DO NOTHING;
