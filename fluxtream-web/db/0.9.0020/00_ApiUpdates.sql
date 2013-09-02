ALTER TABLE `ApiUpdates` ADD COLUMN httpResponseCode int NULL;
ALTER TABLE `ApiUpdates` ADD COLUMN reason mediumtext NULL;
ALTER TABLE `ApiUpdates` ADD INDEX `httpResponseCode` (`httpResponseCode`);
