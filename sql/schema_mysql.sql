-- FitSense desktop client — minimal MySQL schema (Symfony-compatible names).
-- Run as a user that can create tables in `fitsense`, e.g.:
--   mysql -u root -p -h 127.0.0.1 -P 3308 fitsense < schema_mysql.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_user` (
  `id` BINARY(16) NOT NULL,
  `email_email` VARCHAR(255) NOT NULL,
  `roles` LONGTEXT NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `name_firstname` VARCHAR(255) DEFAULT NULL,
  `name_lastname` VARCHAR(255) DEFAULT NULL,
  `account_status` VARCHAR(50) DEFAULT NULL,
  `date_creation` DATETIME DEFAULT NULL,
  `google_authenticator_secret` VARCHAR(255) DEFAULT NULL,
  `phone_number` VARCHAR(35) DEFAULT NULL,
  `photo` VARCHAR(255) DEFAULT NULL,
  `username` VARCHAR(180) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_app_user_email` (`email_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `profile_physique` (
  `id` BINARY(16) NOT NULL,
  `weight` FLOAT DEFAULT NULL,
  `height` FLOAT DEFAULT NULL,
  `gender` VARCHAR(32) DEFAULT NULL,
  `user_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_profile_physique_user` (`user_id`),
  CONSTRAINT `fk_profile_physique_user`
    FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `objectif_sportif` (
  `id` BINARY(16) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `profile_physique_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_objectif_profile` (`profile_physique_id`),
  CONSTRAINT `fk_objectif_profile_physique`
    FOREIGN KEY (`profile_physique_id`) REFERENCES `profile_physique` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
