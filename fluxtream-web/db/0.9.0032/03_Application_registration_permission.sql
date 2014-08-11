ALTER TABLE Application add column `registrationAllowed` char(1) NOT NULL;
UPDATE Application set registrationAllowed='N';