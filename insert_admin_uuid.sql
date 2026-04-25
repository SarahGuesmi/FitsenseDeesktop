INSERT INTO app_user (id, email_email, password, roles, name_firstname, name_lastname, account_status, date_creation)
VALUES (UNHEX(REPLACE(UUID(),'-','')), 'admin@fitsense.com', '$2a$10$GMLhHBUzbNew9vTBvDJz3erfNiiaCofjYEBAupOmRdqrgUZ3l6swK', '["ROLE_ADMIN"]', 'Admin', 'FitSense', 'active', NOW());
