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

-- Coach-authored mental tests (was file-backed; now MySQL)
CREATE TABLE IF NOT EXISTS `coach_mental_test` (
  `id` BINARY(16) NOT NULL,
  `coach_user_id` BINARY(16) NOT NULL,
  `title` VARCHAR(512) NOT NULL,
  `created_at` DATETIME(6) DEFAULT NULL,
  `updated_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_coach_mental_test_coach` (`coach_user_id`),
  CONSTRAINT `fk_coach_mental_test_user`
    FOREIGN KEY (`coach_user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `coach_mental_test_question` (
  `id` BINARY(16) NOT NULL,
  `test_id` BINARY(16) NOT NULL,
  `order_index` INT NOT NULL,
  `prompt` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_coach_q_test` (`test_id`),
  CONSTRAINT `fk_coach_q_test`
    FOREIGN KEY (`test_id`) REFERENCES `coach_mental_test` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Member mental evaluations (check-ins)
CREATE TABLE IF NOT EXISTS `mental_health_evaluation` (
  `id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `coach_test_id` BINARY(16) DEFAULT NULL,
  `coach_test_title` VARCHAR(512) DEFAULT NULL,
  `tested_at` DATETIME(6) DEFAULT NULL,
  `mood` INT NOT NULL DEFAULT 0,
  `stress` INT NOT NULL DEFAULT 0,
  `sleep` INT NOT NULL DEFAULT 0,
  `motivation` INT NOT NULL DEFAULT 0,
  `mental_tired` INT NOT NULL DEFAULT 0,
  `score` INT NOT NULL,
  `status` VARCHAR(64) DEFAULT NULL,
  `member_notes` TEXT,
  `question_scores_json` TEXT,
  `question_prompts_json` TEXT,
  PRIMARY KEY (`id`),
  KEY `idx_mh_eval_user` (`user_id`),
  KEY `idx_mh_eval_tested` (`tested_at`),
  CONSTRAINT `fk_mh_eval_user`
    FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Coach dashboard feed / member recommendations (one row per evaluation check-in)
CREATE TABLE IF NOT EXISTS `mental_health_submission` (
  `id` BINARY(16) NOT NULL,
  `evaluation_id` BINARY(16) NOT NULL,
  `user_id` BINARY(16) NOT NULL,
  `user_full_name` VARCHAR(512) DEFAULT NULL,
  `user_email` VARCHAR(255) DEFAULT NULL,
  `tested_at` DATETIME(6) DEFAULT NULL,
  `stress` INT NOT NULL DEFAULT 0,
  `sleep` INT NOT NULL DEFAULT 0,
  `mood` INT NOT NULL DEFAULT 0,
  `motivation` INT NOT NULL DEFAULT 0,
  `mental_tired` INT NOT NULL DEFAULT 0,
  `score` INT NOT NULL,
  `status` VARCHAR(64) DEFAULT NULL,
  `member_notes` TEXT,
  `coach_test_title` VARCHAR(512) DEFAULT NULL,
  `coach_recommendation` TEXT,
  `recommendation_general_note` TEXT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_mh_submission_eval` (`evaluation_id`),
  KEY `idx_mh_sub_user` (`user_id`),
  KEY `idx_mh_sub_tested` (`tested_at`),
  CONSTRAINT `fk_mh_sub_user`
    FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mh_sub_eval`
    FOREIGN KEY (`evaluation_id`) REFERENCES `mental_health_evaluation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `mental_health_recommended_exercise` (
  `id` BINARY(16) NOT NULL,
  `submission_id` BINARY(16) NOT NULL,
  `sort_order` INT NOT NULL,
  `name` VARCHAR(512) NOT NULL,
  `duration_minutes` VARCHAR(64) DEFAULT NULL,
  `description` TEXT,
  PRIMARY KEY (`id`),
  KEY `idx_mh_ex_sub` (`submission_id`),
  CONSTRAINT `fk_mh_ex_submission`
    FOREIGN KEY (`submission_id`) REFERENCES `mental_health_submission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
