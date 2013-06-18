ALTER TABLE `Facet_Location` ADD COLUMN `date` varchar(10) NULL;
ALTER TABLE `Facet_Location` ADD INDEX `date` (`date`);
ALTER TABLE `Facet_Location` ADD COLUMN `isLocalTime` char(1) NOT NULL DEFAULT 'N';
ALTER TABLE `Facet_Location` ADD INDEX `isLocalTime` (`isLocalTime`);
