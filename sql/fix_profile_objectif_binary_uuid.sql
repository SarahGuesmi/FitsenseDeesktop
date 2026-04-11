-- Fix "Out of range value for column 'id'" when saving onboarding / objectives.
-- The Java app stores UUIDs as BINARY(16). id / user_id / profile_physique_id must be BINARY(16), not INT.
--
-- Run against your `fitsense` database (adjust -P 3308 if needed):
--   mysql -u root -p -h 127.0.0.1 -P 3308 fitsense < fix_profile_objectif_binary_uuid.sql
--
-- WARNING: drops data in objectif_sportif and profile_physique. Skip DROP if you need to keep rows.

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `objectif_sportif`;
DROP TABLE IF EXISTS `profile_physique`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `profile_physique` (
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

CREATE TABLE `objectif_sportif` (
  `id` BINARY(16) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `profile_physique_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_objectif_profile` (`profile_physique_id`),
  CONSTRAINT `fk_objectif_profile_physique`
    FOREIGN KEY (`profile_physique_id`) REFERENCES `profile_physique` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
