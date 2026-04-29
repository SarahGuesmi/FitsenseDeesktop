INSERT INTO app_user (id, email_email, password, roles, name_firstname, name_lastname, account_status, date_creation, username)
VALUES (
  UNHEX(REPLACE(UUID(), '-', '')),
  'sarahguesmi223@gmail.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  '["ROLE_ADMIN"]',
  'Admin',
  'FitSense',
  'active',
  NOW(),
  'admin_fitsense'
)
ON DUPLICATE KEY UPDATE roles='["ROLE_ADMIN"]', account_status='active';
