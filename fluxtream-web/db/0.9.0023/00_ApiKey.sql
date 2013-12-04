ALTER TABLE `ApiKey` ADD COLUMN `synching` char(1) COLLATE utf8mb4_unicode_ci NOT NULL;
UPDATE `ApiKey` SET synching='N';