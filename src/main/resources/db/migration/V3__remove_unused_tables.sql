-- Drop Spring Session tables if they exist and are not being used
DROP TABLE IF EXISTS SPRING_SESSION;
DROP TABLE IF EXISTS SPRING_SESSION_ATTRIBUTES;

-- Note: The following tables are in active use and should NOT be dropped:
-- activities
-- device_fingerprint
-- transaction
-- useraccount
-- ussd_user_accounts