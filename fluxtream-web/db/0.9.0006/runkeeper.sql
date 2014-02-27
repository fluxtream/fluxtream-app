DROP TABLE IF EXISTS `FitnessActivityHeartRate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FitnessActivityHeartRate` (
  `FitnessActivityID` bigint(20) NOT NULL,
  `heartRate` double NOT NULL,
  `timestamp` double NOT NULL,
  KEY `FK64EE970F6B3E8E7` (`FitnessActivityID`),
  CONSTRAINT `FK64EE970F6B3E8E7` FOREIGN KEY (`FitnessActivityID`) REFERENCES `Facet_RunKeeperFitnessActivity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `Facet_RunKeeperFitnessActivity`
--

DROP TABLE IF EXISTS `Facet_RunKeeperFitnessActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_RunKeeperFitnessActivity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `comments` varchar(255) DEFAULT NULL,
  `duration` int(11) NOT NULL,
  `equipment` varchar(255) DEFAULT NULL,
  `is_live` bit(1) NOT NULL,
  `source` varchar(255) DEFAULT NULL,
  `total_climb` double NOT NULL,
  `total_distance` double NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  `uri` varchar(255) DEFAULT NULL,
  `userID` varchar(255) DEFAULT NULL,
  `timeZone` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=latin1;


ALTER TABLE `Facet_GoogleLatitudeLocation` ADD COLUMN  `uri` varchar(255) DEFAULT NULL;
ALTER TABLE `Facet_GoogleLatitudeLocation` ADD INDEX `uri` (`uri`);
