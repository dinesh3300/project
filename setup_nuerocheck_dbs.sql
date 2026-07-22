-- ============================================================
-- Nuerocheck Unified Database Setup Script
-- One shared database for Android and Web.
-- ============================================================

CREATE DATABASE IF NOT EXISTS `nuerocheck_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `nuerocheck_db`;

-- 1. Doctors Table (Shared Users)
CREATE TABLE IF NOT EXISTS `doctors` (
  `id`            INT           AUTO_INCREMENT PRIMARY KEY,
  `name`          VARCHAR(100)  NOT NULL,
  `email`         VARCHAR(100)  UNIQUE NOT NULL,
  `mobile`        VARCHAR(20)   NOT NULL,
  `gender`        VARCHAR(10)   NOT NULL,
  `password`      VARCHAR(255)  NOT NULL,
  `specialty`     VARCHAR(100)  DEFAULT 'General Radiologist',
  `hospital`      VARCHAR(150)  DEFAULT NULL,
  `license_no`    VARCHAR(50)   DEFAULT NULL,
  `experience`    INT           DEFAULT 0,
  `dob`           DATE          DEFAULT NULL,
  `address`       TEXT          DEFAULT NULL,
  `profile_image` VARCHAR(255)  DEFAULT NULL,
  `theme`         VARCHAR(10)   DEFAULT 'light', -- 'light' or 'dark'
  `language`      VARCHAR(5)    DEFAULT 'en',    -- 'en', 'hi', 'bn', etc.
  `daily_summary` BOOLEAN       DEFAULT TRUE,
  `notif_sound`   BOOLEAN       DEFAULT TRUE,
  `notif_vibration` BOOLEAN     DEFAULT TRUE,
  `created_at`    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  `last_login`    TIMESTAMP     NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. OTP Verifications
CREATE TABLE IF NOT EXISTS `otp_verifications` (
  `id`         INT          AUTO_INCREMENT PRIMARY KEY,
  `email`      VARCHAR(255) NOT NULL,
  `otp_code`   VARCHAR(10)  NOT NULL,
  `action`     VARCHAR(50)  NOT NULL, -- 'signup', 'forgot_pwd'
  `created_at` TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME     NOT NULL,
  INDEX `idx_email_action` (`email`, `action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Scans Table
CREATE TABLE IF NOT EXISTS `scans` (
  `id`               INT          AUTO_INCREMENT PRIMARY KEY,
  `doctor_id`        INT          NOT NULL,
  `doctor_email`     VARCHAR(100) NOT NULL, -- Keep email for easier lookups
  `patient_id`       VARCHAR(100) NOT NULL,
  `patient_name`     VARCHAR(100) NOT NULL,
  `patient_age`      INT          NOT NULL,
  `patient_gender`   VARCHAR(10)  NOT NULL,
  `result`           VARCHAR(50)  NOT NULL, -- 'Hemorrhage' or 'Normal'
  `risk_level`       VARCHAR(20)  NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
  `image_path`       VARCHAR(255) NOT NULL,
  `notes`            TEXT         DEFAULT NULL,
  `doctor_review`    TEXT         DEFAULT NULL,
  `intraventricular` DOUBLE       NOT NULL DEFAULT 0.0,
  `intraparenchymal` DOUBLE       NOT NULL DEFAULT 0.0,
  `subarachnoid`     DOUBLE       NOT NULL DEFAULT 0.0,
  `epidural`         DOUBLE       NOT NULL DEFAULT 0.0,
  `subdural`         DOUBLE       NOT NULL DEFAULT 0.0,
  `created_at`       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_doctor_id`    (`doctor_id`),
  INDEX `idx_doctor_email` (`doctor_email`),
  INDEX `idx_patient_id`   (`patient_id`),
  FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Reports Table
CREATE TABLE IF NOT EXISTS `reports` (
  `id`               INT           AUTO_INCREMENT PRIMARY KEY,
  `scan_id`          INT           NOT NULL,
  `pdf_path`         VARCHAR(255)  DEFAULT NULL,
  `clinical_summary` TEXT          DEFAULT NULL,
  `recommendations`  TEXT          DEFAULT NULL,
  `created_at`       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`scan_id`) REFERENCES `scans`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Notifications Table
CREATE TABLE IF NOT EXISTS `notifications` (
  `id`          INT           AUTO_INCREMENT PRIMARY KEY,
  `doctor_id`   INT           NOT NULL,
  `title`       VARCHAR(150)  NOT NULL,
  `message`     TEXT          NOT NULL,
  `type`        VARCHAR(50)   DEFAULT 'general', -- 'scan_complete', 'critical_alert', 'system'
  `is_read`     BOOLEAN       DEFAULT FALSE,
  `created_at`  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`doctor_id`) REFERENCES `doctors`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
