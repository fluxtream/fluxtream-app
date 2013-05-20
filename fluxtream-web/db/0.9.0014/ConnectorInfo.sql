ALTER TABLE `Connector` ADD COLUMN `supportsRenewTokens` char(1) NOT NULL DEFAULT 'N';
DELETE FROM `Connector`;