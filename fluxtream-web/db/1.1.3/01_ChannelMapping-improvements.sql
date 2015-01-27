ALTER TABLE ChannelMapping ADD COLUMN `creationType` TINYINT;
UPDATE ChannelMapping set creationType=0;
ALTER TABLE ChannelMapping CHANGE objectTypeId objectTypes INT;