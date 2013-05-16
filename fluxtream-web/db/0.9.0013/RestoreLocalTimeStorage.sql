DROP PROCEDURE IF EXISTS RestoreFitbitActivityLocalTimeStorage;
DROP PROCEDURE IF EXISTS RestoreFitbitLoggedActivitiesLocalTimeStorage;
DROP PROCEDURE IF EXISTS RestoreFitbitSleepLocalTimeStorage;
DROP PROCEDURE IF EXISTS RestoreFitbitWeightLocalTimeStorage;
DROP PROCEDURE IF EXISTS RestoreFlickrPhotoLocalTimeStorage;
DROP PROCEDURE IF EXISTS RestoreZeoSleepStatsLocalTimeStorage;

DELIMITER $$
CREATE PROCEDURE RestoreFitbitActivityLocalTimeStorage()
  BEGIN
    DECLARE _count INT;
    SET _count = (  SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE   TABLE_SCHEMA = 'flx' AND
                            TABLE_NAME = 'Facet_FitbitActivity' AND
                            COLUMN_NAME = 'startTimeStorage');
    IF _count = 0 THEN
      ALTER TABLE Facet_FitbitActivity
      ADD COLUMN startTimeStorage varchar(255) DEFAULT NULL;
      ALTER TABLE Facet_FitbitActivity
      ADD COLUMN endTimeStorage varchar(255) DEFAULT NULL;
    END IF;
  END $$
DELIMITER ;


DELIMITER $$
CREATE PROCEDURE RestoreFitbitLoggedActivitiesLocalTimeStorage()
  BEGIN
    DECLARE _count INT;
    SET _count = (  SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE   TABLE_SCHEMA = 'flx' AND
                            TABLE_NAME = 'Facet_FitbitLoggedActivity' AND
                            COLUMN_NAME = 'startTimeStorage');
    IF _count = 0 THEN
      ALTER TABLE Facet_FitbitLoggedActivity
      ADD COLUMN startTimeStorage varchar(255) DEFAULT NULL;
      ALTER TABLE Facet_FitbitLoggedActivity
      ADD COLUMN endTimeStorage varchar(255) DEFAULT NULL;
    END IF;
  END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE RestoreFitbitSleepLocalTimeStorage()
  BEGIN
    DECLARE _count INT;
    SET _count = (  SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE   TABLE_SCHEMA = 'flx' AND
                            TABLE_NAME = 'Facet_FitbitSleep' AND
                            COLUMN_NAME = 'startTimeStorage');
    IF _count = 0 THEN
      ALTER TABLE Facet_FitbitSleep
      ADD COLUMN startTimeStorage varchar(255) DEFAULT NULL;
      ALTER TABLE Facet_FitbitSleep
      ADD COLUMN endTimeStorage varchar(255) DEFAULT NULL;
    END IF;
  END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE RestoreFitbitWeightLocalTimeStorage()
  BEGIN
    DECLARE _count INT;
    SET _count = (  SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE   TABLE_SCHEMA = 'flx' AND
                            TABLE_NAME = 'Facet_FitbitWeight' AND
                            COLUMN_NAME = 'startTimeStorage');
    IF _count = 0 THEN
      ALTER TABLE Facet_FitbitWeight
      ADD COLUMN startTimeStorage varchar(255) DEFAULT NULL;
      ALTER TABLE Facet_FitbitWeight
      ADD COLUMN endTimeStorage varchar(255) DEFAULT NULL;
    END IF;
  END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE RestoreFlickrPhotoLocalTimeStorage()
  BEGIN
    DECLARE _count INT;
    SET _count = (  SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE   TABLE_SCHEMA = 'flx' AND
                            TABLE_NAME = 'Facet_FlickrPhoto' AND
                            COLUMN_NAME = 'startTimeStorage');
    IF _count = 0 THEN
      ALTER TABLE Facet_FlickrPhoto
      ADD COLUMN startTimeStorage varchar(255) DEFAULT NULL;
      ALTER TABLE Facet_FlickrPhoto
      ADD COLUMN endTimeStorage varchar(255) DEFAULT NULL;
    END IF;
  END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE RestoreZeoSleepStatsLocalTimeStorage()
  BEGIN
    DECLARE _count INT;
    SET _count = (  SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE   TABLE_SCHEMA = 'flx' AND
                            TABLE_NAME = 'Facet_ZeoSleepStats' AND
                            COLUMN_NAME = 'startTimeStorage');
    IF _count = 0 THEN
      ALTER TABLE Facet_ZeoSleepStats
      ADD COLUMN startTimeStorage varchar(255) DEFAULT NULL;
      ALTER TABLE Facet_ZeoSleepStats
      ADD COLUMN endTimeStorage varchar(255) DEFAULT NULL;
    END IF;
  END $$
DELIMITER ;

CALL RestoreFitbitActivityLocalTimeStorage();
CALL RestoreFitbitLoggedActivitiesLocalTimeStorage();
CALL RestoreFitbitSleepLocalTimeStorage();
CALL RestoreFitbitWeightLocalTimeStorage();
CALL RestoreFlickrPhotoLocalTimeStorage();
CALL RestoreZeoSleepStatsLocalTimeStorage();
