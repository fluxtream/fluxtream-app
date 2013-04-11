ALTER TABLE `Facet_GoogleLatitudeLocation` ADD COLUMN `processed` char(1);
ALTER TABLE `Facet_GoogleLatitudeLocation` ADD INDEX `processed_index` (`processed`);
