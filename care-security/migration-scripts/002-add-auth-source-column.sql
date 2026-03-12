-- Add AUTH_SOURCE column to SAUSER table for LDAP integration
-- Values: 'LOCAL' (default, database authentication) or 'LDAP' (LDAP directory authentication)

ALTER TABLE SAUSER ADD AUTH_SOURCE VARCHAR(20) DEFAULT 'LOCAL';

-- Update all existing users to LOCAL
UPDATE SAUSER SET AUTH_SOURCE = 'LOCAL' WHERE AUTH_SOURCE IS NULL;
