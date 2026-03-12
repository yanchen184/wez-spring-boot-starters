-- Migration Script: Remove legacy REQUESTMAP table
-- Purpose: REQUESTMAP is a Grails legacy table no longer used in Spring Boot Security
-- Author: Care Security Team
-- Date: 2026-03-05
-- Issue: Redundant Code Cleanup

-- =============================================================================
-- Step 1: Backup existing data to archive table
-- =============================================================================

IF OBJECT_ID('REQUESTMAP_ARCHIVE', 'U') IS NOT NULL
    DROP TABLE REQUESTMAP_ARCHIVE;
GO

SELECT *
INTO REQUESTMAP_ARCHIVE
FROM REQUESTMAP;
GO

PRINT 'Backed up ' + CAST(@@ROWCOUNT AS NVARCHAR(10)) + ' rows from REQUESTMAP to REQUESTMAP_ARCHIVE';
GO

-- =============================================================================
-- Step 2: Drop the REQUESTMAP table
-- =============================================================================

IF OBJECT_ID('REQUESTMAP', 'U') IS NOT NULL
BEGIN
    DROP TABLE REQUESTMAP;
    PRINT 'REQUESTMAP table dropped successfully';
END
ELSE
BEGIN
    PRINT 'REQUESTMAP table does not exist (already removed)';
END
GO

-- =============================================================================
-- Step 3: (Optional) Remove the RequestMap Entity from JPA
-- =============================================================================

-- After running this script:
-- 1. Delete: care-security-core/src/main/java/gov/mohw/care/security/entity/RequestMap.java
-- 2. If any code references RequestMap, remove those references
-- 3. Run tests to ensure nothing breaks: mvn test
-- 4. Commit changes with message: "chore: remove deprecated RequestMap entity and table"

-- =============================================================================
-- Rollback Script (if needed)
-- =============================================================================

-- To restore the table:
-- SELECT * INTO REQUESTMAP FROM REQUESTMAP_ARCHIVE;
-- Then restore RequestMap.java from git history

-- =============================================================================
-- Verification
-- =============================================================================

-- Check backup table exists and has data
SELECT
    'REQUESTMAP_ARCHIVE' AS TableName,
    COUNT(*) AS RowCount,
    MIN(ID) AS MinId,
    MAX(ID) AS MaxId
FROM REQUESTMAP_ARCHIVE;
GO

-- Verify original table is dropped
IF OBJECT_ID('REQUESTMAP', 'U') IS NULL
    PRINT '✅ REQUESTMAP table successfully removed';
ELSE
    PRINT '❌ REQUESTMAP table still exists (manual intervention required)';
GO
