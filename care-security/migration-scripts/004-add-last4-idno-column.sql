-- Add LAST4_IDNO column to SAUSER table for MOICA citizen certificate integration
-- Stores the last 4 digits of the national ID number extracted from the certificate extension OID 2.16.886.1.100.1.1
-- Used together with DISPLAY_NAME (cname) for user matching during PKCS#7 cert login

-- For MSSQL
ALTER TABLE SAUSER ADD LAST4_IDNO VARCHAR(4) NULL;

-- Create index for the cname + last4_idno lookup pattern
CREATE INDEX IDX_SAUSER_CNAME_LAST4IDNO ON SAUSER (DISPLAY_NAME, LAST4_IDNO);
