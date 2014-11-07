ALTER TABLE FitbitUserProfile ADD COLUMN `apiKeyId` bigint(20) NULL;
ALTER TABLE FitbitUserProfile DROP COLUMN `guestId`;
ALTER TABLE FitbitUserProfile DROP COLUMN `firstSeenHere`;
