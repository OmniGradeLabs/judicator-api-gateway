-- Seed RBAC and initial system identity for the Judicator API Gateway.
-- Legacy parking-domain roles, permissions, tenants, and device records have been removed.
-- Default password for all seeded users: Password@123

-- ============================================================
-- System-level permissions (HE_THONG module only)
-- ============================================================
INSERT INTO permissions (id, name, scope, module, resource, label, action)
VALUES
    (
        md5('QUYEN_TREN_APP.HE_THONG.TENANT.CREATE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.TENANT.CREATE',
        'Quyền trên app',
        'HE_THONG',
        'Tenant',
        'Khách hàng SaaS',
        'CREATE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.TENANT.VIEW')::uuid,
        'QUYEN_TREN_APP.HE_THONG.TENANT.VIEW',
        'Quyền trên app',
        'HE_THONG',
        'Tenant',
        'Khách hàng SaaS',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.TENANT.UPDATE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.TENANT.UPDATE',
        'Quyền trên app',
        'HE_THONG',
        'Tenant',
        'Khách hàng SaaS',
        'UPDATE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.TENANT.DELETE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.TENANT.DELETE',
        'Quyền trên app',
        'HE_THONG',
        'Tenant',
        'Khách hàng SaaS',
        'DELETE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.USER.MANAGE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.USER.MANAGE',
        'Quyền trên app',
        'HE_THONG',
        'User',
        'Tài khoản người dùng',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.ROLE.MANAGE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.ROLE.MANAGE',
        'Quyền trên app',
        'HE_THONG',
        'Role',
        'Vai trò',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.PERMISSION.MANAGE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.PERMISSION.MANAGE',
        'Quyền trên app',
        'HE_THONG',
        'Permission',
        'Quyền hạn',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.CONFIG.MANAGE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.CONFIG.MANAGE',
        'Quyền trên app',
        'HE_THONG',
        'Config',
        'Cấu hình hệ thống',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.AUDIT_LOG.VIEW')::uuid,
        'QUYEN_TREN_APP.HE_THONG.AUDIT_LOG.VIEW',
        'Quyền trên app',
        'HE_THONG',
        'AuditLog',
        'Nhật ký hệ thống',
        'VIEW'
    )
ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- Roles — only SYSTEM_ADMIN remains for this gateway
-- ============================================================
INSERT INTO roles (id, name, "desc")
VALUES
    (
        md5('SYSTEM_ADMIN')::uuid,
        'SYSTEM_ADMIN',
        'System Admin — manages tenants, users, roles, permissions, and system configuration'
    )
ON CONFLICT (name) DO NOTHING;

-- Grant all system permissions to SYSTEM_ADMIN
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT md5(r.name || ':' || p.name)::uuid, r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- Seed tenant: judicator-saas (platform owner)
-- ============================================================
INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted)
VALUES
    (
        md5('judicator-saas')::uuid,
        'Judicator SaaS',
        'judicator-saas',
        'admin@judicator.local',
        'ACTIVE',
        false
    )
ON CONFLICT (slug) DO NOTHING;

-- ============================================================
-- Seed user: system admin
-- Default password: Password@123
-- ============================================================
INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
VALUES
    (
        md5('system.admin@judicator.local')::uuid,
        md5('judicator-saas')::uuid,
        'system.admin@judicator.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'System Administrator',
        '0900000001',
        'ACTIVE',
        NULL,
        false
    )
ON CONFLICT (username) DO NOTHING;

-- Assign SYSTEM_ADMIN role
INSERT INTO user_roles (id, user_id, role_id)
VALUES
    (
        md5('system.admin@judicator.local:SYSTEM_ADMIN')::uuid,
        md5('system.admin@judicator.local')::uuid,
        md5('SYSTEM_ADMIN')::uuid
    )
ON CONFLICT (user_id, role_id) DO NOTHING;
