ALTER TABLE Settings ADD COLUMN `preferences` MEDIUMTEXT;
UPDATE Settings SET preferences='{"heatMap":{"DAY":false,"WEEK":true,"MONTH":true}}';