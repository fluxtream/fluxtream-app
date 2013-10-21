DROP TABLE IF EXISTS FitnessActivityDistance;
DROP TABLE IF EXISTS FitnessActivityHeartRate;
ALTER TABLE Facet_RunKeeperFitnessActivity ADD COLUMN distanceStorage longtext null;
ALTER TABLE Facet_RunKeeperFitnessActivity ADD COLUMN heartRateStorage longtext null;
ALTER TABLE Facet_RunKeeperFitnessActivity ADD COLUMN caloriesStorage longtext null;

ALTER TABLE Facet_RunKeeperFitnessActivity ADD COLUMN totalCalories int(11) null;
ALTER TABLE Facet_RunKeeperFitnessActivity ADD COLUMN averageHeartRate int(11) null;
ALTER TABLE Facet_RunKeeperFitnessActivity ADD COLUMN averagePace float(3) null;
