-- Add deleted and version columns to all auditable tables
-- Required for common-jpa BaseEntity integration (soft delete + optimistic locking)

-- SAUSER
ALTER TABLE SAUSER ADD deleted BIT NOT NULL DEFAULT 0;
ALTER TABLE SAUSER ADD version INT DEFAULT 0;
UPDATE SAUSER SET version = 0 WHERE version IS NULL;

-- MENU
ALTER TABLE MENU ADD deleted BIT NOT NULL DEFAULT 0;
ALTER TABLE MENU ADD version INT DEFAULT 0;
UPDATE MENU SET version = 0 WHERE version IS NULL;

-- ROLE
ALTER TABLE ROLE ADD deleted BIT NOT NULL DEFAULT 0;
ALTER TABLE ROLE ADD version INT DEFAULT 0;
UPDATE ROLE SET version = 0 WHERE version IS NULL;

-- PERM (already has VERSION column, skip version; no deleted)
ALTER TABLE PERM ADD deleted BIT NOT NULL DEFAULT 0;
-- Note: PERM.VERSION already exists, will be reused as @Version optimistic lock

-- ORGANIZE (already has IS_DEL, migrate to deleted)
-- Step 1: Add new column
ALTER TABLE ORGANIZE ADD deleted BIT NOT NULL DEFAULT 0;
-- Step 2: Migrate existing data
UPDATE ORGANIZE SET deleted = IS_DEL WHERE IS_DEL IS NOT NULL;
-- Step 3: Drop old column (optional — can keep for backward compatibility)
-- ALTER TABLE ORGANIZE DROP COLUMN IS_DEL;
ALTER TABLE ORGANIZE ADD version INT DEFAULT 0;
UPDATE ORGANIZE SET version = 0 WHERE version IS NULL;

-- PWD_HISTORY
ALTER TABLE PWD_HISTORY ADD deleted BIT NOT NULL DEFAULT 0;
ALTER TABLE PWD_HISTORY ADD version INT DEFAULT 0;
UPDATE PWD_HISTORY SET version = 0 WHERE version IS NULL;

-- SAUSER_ORG_ROLE
ALTER TABLE SAUSER_ORG_ROLE ADD deleted BIT NOT NULL DEFAULT 0;
ALTER TABLE SAUSER_ORG_ROLE ADD version INT DEFAULT 0;
UPDATE SAUSER_ORG_ROLE SET version = 0 WHERE version IS NULL;
