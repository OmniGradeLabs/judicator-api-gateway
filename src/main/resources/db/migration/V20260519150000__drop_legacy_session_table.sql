-- Migration: Drop the legacy PostgreSQL 'sessions' table.
-- Session management has been fully migrated to MongoDB (Spring Data MongoDB).
-- MongoDB is now the sole Source of Truth for active session lifecycle data.
-- Reference: Polyglot Persistence migration — sessions collection in 'judicator_session' DB.
ALTER TABLE sessions DROP CONSTRAINT IF EXISTS fk_sessions_user;
DROP TABLE IF EXISTS sessions;
