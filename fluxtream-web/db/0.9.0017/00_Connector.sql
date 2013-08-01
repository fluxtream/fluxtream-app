ALTER TABLE `Connector` ADD COLUMN `supportsSync` char(1) NOT NULL DEFAULT 'Y';
ALTER TABLE `Connector` ADD COLUMN `supportsFileUpload` char(1) NOT NULL DEFAULT 'N';
ALTER TABLE `Connector` ADD COLUMN `apiKeyAttributeKeys` longtext NULL;
DELETE FROM `Connector`;
