-- Delete all existing admin users
DELETE FROM useraccount WHERE role = 'ADMIN';

-- Insert single admin user with bcrypt hashed password (admin123)
INSERT INTO useraccount (username, password, email, phonenumber, role, account_status, created_at, last_updated)
VALUES (
    'admin',
    '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', -- bcrypt hash of 'admin123'
    'admin@example.com',
    '0700000000',
    'ADMIN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);