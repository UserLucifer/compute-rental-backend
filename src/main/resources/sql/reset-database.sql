-- Recreate the compute_rental database from scratch.
-- Destructive: this drops all existing data in compute_rental.
DROP DATABASE IF EXISTS `compute_rental`;
CREATE DATABASE `compute_rental`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE `compute_rental`;

SOURCE schema.sql;
SOURCE init-data.sql;
