-- Check if admin user exists
INSERT INTO useraccount (username, password, email, phonenumber, role, account_status)
SELECT 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'admin@example.com', '1234567890', 'ADMIN', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM useraccount WHERE username = 'admin'
);
