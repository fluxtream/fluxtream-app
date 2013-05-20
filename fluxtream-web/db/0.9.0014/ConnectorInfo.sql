ALTER TABLE `Connector` ADD COLUMN `supportsRenewToken` char(1) NOT NULL DEFAULT 'N';
DELETE FROM `Connector`;