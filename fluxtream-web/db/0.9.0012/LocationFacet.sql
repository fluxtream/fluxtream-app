ALTER TABLE `Facet_GoogleLatitudeLocation` ADD COLUMN `processed` char(1);
ALTER TABLE `Facet_GoogleLatitudeLocation` ADD INDEX `processed_index` (`processed`);
CREATE TABLE `DatabaseMetadata` (
  `version` varchar(255) NOT NULL
);
INSERT INTO DatabaseMetadata (version) VALUES ('0.9.0012');