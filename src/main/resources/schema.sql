-- ============================================================
-- Runs AFTER Hibernate ddl-auto=update
-- (spring.jpa.defer-datasource-initialization=true)
--
-- Hibernate never UPDATES existing check constraints,
-- so we drop the stale one and recreate it with all 4 values.
-- ============================================================

-- Step 1: drop the stale constraint (safe if it doesn't exist)
ALTER TABLE task DROP CONSTRAINT IF EXISTS task_status_check;

-- Step 2: recreate with the CORRECT set of values including REVIEW
ALTER TABLE task ADD CONSTRAINT task_status_check
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'));

-- Step 3: add priority constraint (idempotent)
ALTER TABLE task DROP CONSTRAINT IF EXISTS task_priority_check;
ALTER TABLE task ADD CONSTRAINT task_priority_check
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

