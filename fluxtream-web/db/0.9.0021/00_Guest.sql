ALTER TABLE `Guest` ADD COLUMN registrationMethod TINYINT;
UPDATE `Guest` SET registrationMethod = 0;
ALTER TABLE `Guest` ADD COLUMN autoLoginToken VARCHAR(255) NULL;
ALTER TABLE `Guest` ADD COLUMN autoLoginTokenTimestamp BIGINT(21) NULL;