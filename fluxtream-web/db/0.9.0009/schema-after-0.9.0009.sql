-- MySQL dump 10.13  Distrib 5.5.25, for osx10.6 (i386)
--
-- Host: localhost    Database: mojo
-- ------------------------------------------------------
-- Server version	5.5.25

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Address`
--

DROP TABLE IF EXISTS `Address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Address` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `jsonStorage` longtext,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `radius` double NOT NULL,
  `since` bigint(20) NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  `until` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ApiKey`
--

DROP TABLE IF EXISTS `ApiKey`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ApiKey` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `guestId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `api_index` (`api`),
  KEY `guestId_index` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=30 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ApiKeyAttribute`
--

DROP TABLE IF EXISTS `ApiKeyAttribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ApiKeyAttribute` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `attributeKey` varchar(255) DEFAULT NULL,
  `attributeValue` longtext,
  `apiKey_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK63B21617FB39AB73` (`apiKey_id`)
) ENGINE=MyISAM AUTO_INCREMENT=6007 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ApiNotifications`
--

DROP TABLE IF EXISTS `ApiNotifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ApiNotifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `content` longtext,
  `guestId` bigint(20) NOT NULL,
  `ts` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ts` (`ts`),
  KEY `guestId` (`guestId`),
  KEY `api` (`api`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ApiUpdates`
--

DROP TABLE IF EXISTS `ApiUpdates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ApiUpdates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `elapsed` bigint(20) NOT NULL,
  `guestId` bigint(20) NOT NULL,
  `objectTypes` int(11) NOT NULL,
  `query` text,
  `success` char(1) NOT NULL,
  `ts` bigint(20) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `elapsed` (`elapsed`),
  KEY `ts` (`ts`),
  KEY `objectTypes` (`objectTypes`),
  KEY `guestId` (`guestId`),
  KEY `api` (`api`),
  KEY `success` (`success`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=65382 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ChannelStyle`
--

DROP TABLE IF EXISTS `ChannelStyle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ChannelStyle` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `channelName` varchar(255) DEFAULT NULL,
  `deviceName` varchar(255) DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `json` longtext,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`),
  KEY `channelName` (`channelName`),
  KEY `deviceName` (`deviceName`)
) ENGINE=MyISAM AUTO_INCREMENT=25 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CoachingBuddies`
--

DROP TABLE IF EXISTS `CoachingBuddies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CoachingBuddies` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guestId` bigint(20) NOT NULL,
  `buddyId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `guestId_index` (`guestId`),
  KEY `buddyId_index` (`buddyId`),
  KEY `buddyId` (`buddyId`),
  KEY `guestId` (`guestId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Connector`
--

DROP TABLE IF EXISTS `Connector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Connector` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `channels` tinyblob,
  `connectUrl` varchar(255) DEFAULT NULL,
  `connectorName` varchar(255) DEFAULT NULL,
  `count` int(11) NOT NULL,
  `enabled` char(1) NOT NULL,
  `image` varchar(255) DEFAULT NULL,
  `manageable` char(1) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `text` longtext,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=16 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ConnectorChannelSet`
--

DROP TABLE IF EXISTS `ConnectorChannelSet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ConnectorChannelSet` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `channels` longtext,
  `guestId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`),
  KEY `api` (`api`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ConnectorFilterState`
--

DROP TABLE IF EXISTS `ConnectorFilterState`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ConnectorFilterState` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guestId` bigint(20) NOT NULL,
  `stateJSON` longtext,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ContextualInfo`
--

DROP TABLE IF EXISTS `ContextualInfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ContextualInfo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `cities` longtext,
  `date` varchar(255) DEFAULT NULL,
  `inTransit` int(11) DEFAULT NULL,
  `maxTempC` float NOT NULL,
  `maxTempF` float NOT NULL,
  `minTempC` float NOT NULL,
  `minTempF` float NOT NULL,
  `otherTimeZone` varchar(255) DEFAULT NULL,
  `timeZone` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `travelType` int(11) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `inTransit_index` (`inTransit`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `travelType_index` (`travelType`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=8451 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Dashboard`
--

DROP TABLE IF EXISTS `Dashboard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Dashboard` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `active` char(1) NOT NULL,
  `guestId` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `ordering` int(11) NOT NULL,
  `widgetNames` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DashboardWidgetsRepository`
--

DROP TABLE IF EXISTS `DashboardWidgetsRepository`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DashboardWidgetsRepository` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DatabaseHit`
--

DROP TABLE IF EXISTS `DatabaseHit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DatabaseHit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `barcode` varchar(255) NOT NULL,
  `isGreggLondon` char(1) DEFAULT NULL,
  `isSimpleUPC` char(1) DEFAULT NULL,
  `ts` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `barcode` (`barcode`),
  KEY `isGreggLondon` (`isGreggLondon`),
  KEY `isSimpleUPC` (`isSimpleUPC`)
) ENGINE=MyISAM AUTO_INCREMENT=492 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_BodymediaBurn`
--

DROP TABLE IF EXISTS `Facet_BodymediaBurn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_BodymediaBurn` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `json` longtext,
  `date` varchar(255) DEFAULT NULL,
  `lastSync` bigint(20) NOT NULL,
  `estimatedCalories` int(11) NOT NULL,
  `predictedCalories` int(11) NOT NULL,
  `totalCalories` int(11) NOT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=1002 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_BodymediaSleep`
--

DROP TABLE IF EXISTS `Facet_BodymediaSleep`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_BodymediaSleep` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `json` longtext,
  `date` varchar(255) DEFAULT NULL,
  `lastSync` bigint(20) NOT NULL,
  `efficiency` double NOT NULL,
  `totalLying` int(11) NOT NULL,
  `totalSleeping` int(11) NOT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=592 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_BodymediaSteps`
--

DROP TABLE IF EXISTS `Facet_BodymediaSteps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_BodymediaSteps` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `json` longtext,
  `date` varchar(255) DEFAULT NULL,
  `lastSync` bigint(20) NOT NULL,
  `totalSteps` int(11) NOT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=877 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_CalendarEventEntry`
--

DROP TABLE IF EXISTS `Facet_CalendarEventEntry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_CalendarEventEntry` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `edited` bigint(20) NOT NULL,
  `entryId` varchar(255) DEFAULT NULL,
  `icalUID` varchar(255) DEFAULT NULL,
  `kind` varchar(255) DEFAULT NULL,
  `linkHref` longtext,
  `linkTitle` longtext,
  `locationsStorage` longblob,
  `participantsStorage` longblob,
  `plainTextContent` longtext,
  `published` bigint(20) NOT NULL,
  `summary` longtext,
  `textContent` longtext,
  `title` longtext,
  `whenStorage` longblob,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=559 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_CallLog`
--

DROP TABLE IF EXISTS `Facet_CallLog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_CallLog` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `date` datetime DEFAULT NULL,
  `personName` varchar(255) DEFAULT NULL,
  `personNumber` varchar(255) DEFAULT NULL,
  `seconds` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_FitbitActivity`
--

DROP TABLE IF EXISTS `Facet_FitbitActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_FitbitActivity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `activeScore` int(11) NOT NULL,
  `activityCalories` int(11) NOT NULL,
  `caloriesJson` longtext,
  `caloriesOut` int(11) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `elevation` double DEFAULT NULL,
  `fairlyActiveMinutes` int(11) NOT NULL,
  `floors` int(11) NOT NULL,
  `lightlyActiveDistance` double NOT NULL,
  `lightlyActiveMinutes` int(11) NOT NULL,
  `loggedActivitiesDistance` double NOT NULL,
  `moderatelyActiveDistance` double NOT NULL,
  `sedentaryActiveDistance` double NOT NULL,
  `sedentaryMinutes` int(11) NOT NULL,
  `steps` int(11) NOT NULL,
  `stepsJson` longtext,
  `totalDistance` double NOT NULL,
  `trackerDistance` double NOT NULL,
  `veryActiveDistance` double NOT NULL,
  `veryActiveMinutes` int(11) NOT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=7604 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_FitbitLoggedActivity`
--

DROP TABLE IF EXISTS `Facet_FitbitLoggedActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_FitbitLoggedActivity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `activityId` bigint(20) NOT NULL,
  `activityParentId` bigint(20) NOT NULL,
  `calories` int(11) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `distance` double NOT NULL,
  `duration` int(11) NOT NULL,
  `isFavorite` bit(1) NOT NULL,
  `logId` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `steps` int(11) NOT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=13 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_FitbitSleep`
--

DROP TABLE IF EXISTS `Facet_FitbitSleep`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_FitbitSleep` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `awakeningsCount` int(11) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `isMainSleep` bit(1) NOT NULL,
  `logId` bigint(20) NOT NULL,
  `minutesAfterWakeup` int(11) NOT NULL,
  `minutesAsleep` int(11) NOT NULL,
  `minutesAwake` int(11) NOT NULL,
  `minutesToFallAsleep` int(11) NOT NULL,
  `timeInBed` int(11) NOT NULL,
  `duration` int(11) NOT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=1863504 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_FitbitWeight`
--

DROP TABLE IF EXISTS `Facet_FitbitWeight`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_FitbitWeight` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `bmi` double NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `fat` double NOT NULL,
  `weight` double NOT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=10969 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_FlickrPhoto`
--

DROP TABLE IF EXISTS `Facet_FlickrPhoto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_FlickrPhoto` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `accuracy` int(11) NOT NULL,
  `datetaken` bigint(20) NOT NULL,
  `dateupload` bigint(20) NOT NULL,
  `farm` varchar(255) DEFAULT NULL,
  `flickrId` varchar(255) DEFAULT NULL,
  `isfamily` bit(1) NOT NULL,
  `isfriend` bit(1) NOT NULL,
  `ispublic` bit(1) NOT NULL,
  `latitude` varchar(255) DEFAULT NULL,
  `longitude` varchar(255) DEFAULT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `secret` varchar(255) DEFAULT NULL,
  `server` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `date` varchar(255) DEFAULT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=282 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_FluxtreamCapturePhoto`
--

DROP TABLE IF EXISTS `Facet_FluxtreamCapturePhoto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_FluxtreamCapturePhoto` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guestId` bigint(20) NOT NULL,
  `api` int(11) NOT NULL,
  `objectType` int(11) NOT NULL,
  `hash` char(64) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `comment` longtext,
  `fullTextDescription` longtext,
  `start` bigint(20) NOT NULL,
  `end` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `captureYYYYDDD` char(7) NOT NULL,
  `imageType` char(3) NOT NULL,
  `orientation` int(11) NOT NULL,
  `thumbnail0` blob NOT NULL,
  `thumbnail1` blob NOT NULL,
  `thumbnail2` blob NOT NULL,
  `thumbnail0Width` int(11) NOT NULL,
  `thumbnail0Height` int(11) NOT NULL,
  `thumbnail1Width` int(11) NOT NULL,
  `thumbnail1Height` int(11) NOT NULL,
  `thumbnail2Width` int(11) NOT NULL,
  `thumbnail2Height` int(11) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `tags` longtext,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `heading` float DEFAULT NULL,
  `headingRef` char(2) DEFAULT NULL,
  `altitude` float DEFAULT NULL,
  `altitudeRef` int(11) DEFAULT NULL,
  `gpsPrecision` float DEFAULT NULL,
  `gpsDatestamp` varchar(255) DEFAULT NULL,
  `gpsTimestamp` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `api_index` (`api`),
  KEY `end_index` (`end`),
  KEY `guestId_index` (`guestId`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `objectType_index` (`objectType`),
  KEY `start_index` (`start`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `hash_index` (`hash`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_GithubPush`
--

DROP TABLE IF EXISTS `Facet_GithubPush`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_GithubPush` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `commmitsJSON` longtext,
  `repoName` varchar(255) DEFAULT NULL,
  `repoURL` varchar(255) DEFAULT NULL,
  `commitsJSON` longtext,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=1553 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_GoogleLatitudeLocation`
--

DROP TABLE IF EXISTS `Facet_GoogleLatitudeLocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_GoogleLatitudeLocation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `accuracy` int(11) NOT NULL,
  `altitude` int(11) NOT NULL,
  `altitudeAccuracy` int(11) NOT NULL,
  `device` varchar(255) DEFAULT NULL,
  `heading` int(11) NOT NULL,
  `latitude` float NOT NULL,
  `longitude` float NOT NULL,
  `os` varchar(255) DEFAULT NULL,
  `placeid` int(11) NOT NULL,
  `source` int(11) DEFAULT NULL,
  `speed` int(11) NOT NULL,
  `timestampMs` bigint(20) NOT NULL,
  `version` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `date` varchar(10) DEFAULT NULL,
  `timezone` varchar(256) DEFAULT NULL,
  `timezoneMinutesOffset` int(11) DEFAULT NULL,
  `uri` varchar(255) DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `longitude_index` (`longitude`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `latitude_index` (`latitude`),
  KEY `timestamp_index` (`timestampMs`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `uri` (`uri`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=858039 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_InstagramPhoto`
--

DROP TABLE IF EXISTS `Facet_InstagramPhoto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_InstagramPhoto` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `caption` varchar(255) DEFAULT NULL,
  `filter` varchar(255) DEFAULT NULL,
  `instagramId` varchar(255) DEFAULT NULL,
  `latitude` double NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  `locationId` varchar(255) DEFAULT NULL,
  `locationName` varchar(255) DEFAULT NULL,
  `longitude` double NOT NULL,
  `lowResolutionHeight` int(11) NOT NULL,
  `lowResolutionUrl` varchar(255) DEFAULT NULL,
  `lowResolutionWidth` int(11) NOT NULL,
  `standardResolutionHeight` int(11) NOT NULL,
  `standardResolutionUrl` varchar(255) DEFAULT NULL,
  `standardResolutionWidth` int(11) NOT NULL,
  `thumbnailHeight` int(11) NOT NULL,
  `thumbnailUrl` varchar(255) DEFAULT NULL,
  `thumbnailWidth` int(11) NOT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_LastFmLovedTrack`
--

DROP TABLE IF EXISTS `Facet_LastFmLovedTrack`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_LastFmLovedTrack` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `album_mbid` varchar(255) DEFAULT NULL,
  `artist` varchar(255) DEFAULT NULL,
  `artist_mbid` varchar(255) DEFAULT NULL,
  `imgUrls` longtext,
  `name` varchar(255) DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=261 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_LastFmRecentTrack`
--

DROP TABLE IF EXISTS `Facet_LastFmRecentTrack`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_LastFmRecentTrack` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `album_mbid` varchar(255) DEFAULT NULL,
  `artist` varchar(255) DEFAULT NULL,
  `artist_mbid` varchar(255) DEFAULT NULL,
  `imgUrls` longtext,
  `name` varchar(255) DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=83189 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_MymeeObservation`
--

DROP TABLE IF EXISTS `Facet_MymeeObservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_MymeeObservation` (
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
  `note` longtext,
  `mymeeId` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `timezoneOffset` int(11) NOT NULL,
  `amount` double DEFAULT NULL,
  `baseAmount` int(11) DEFAULT NULL,
  `unit` varchar(255) DEFAULT NULL,
  `baseUnit` varchar(255) DEFAULT NULL,
  `imageURL` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `name` (`name`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_PicasaPhotoEntry`
--

DROP TABLE IF EXISTS `Facet_PicasaPhotoEntry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_PicasaPhotoEntry` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `description` longtext,
  `photoId` varchar(255) DEFAULT NULL,
  `photoUrl` varchar(255) DEFAULT NULL,
  `thumbnailUrl` varchar(255) DEFAULT NULL,
  `thumbnailsJson` longtext,
  `title` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=12574 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_QuantifiedMindTest`
--

DROP TABLE IF EXISTS `Facet_QuantifiedMindTest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_QuantifiedMindTest` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `result_name` varchar(255) DEFAULT NULL,
  `result_value` double NOT NULL,
  `session_timestamp` bigint(20) NOT NULL,
  `test_name` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=13 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_SmsEntry`
--

DROP TABLE IF EXISTS `Facet_SmsEntry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_SmsEntry` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `dateReceived` datetime DEFAULT NULL,
  `message` longtext,
  `personName` varchar(255) DEFAULT NULL,
  `personNumber` varchar(255) DEFAULT NULL,
  `type` int(11) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_ToodledoGoal`
--

DROP TABLE IF EXISTS `Facet_ToodledoGoal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_ToodledoGoal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `archived` tinyint(4) NOT NULL,
  `contributes` bigint(20) NOT NULL,
  `level` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `note` longtext,
  `toodledo_id` bigint(20) NOT NULL,
  `tags` longtext,
  `apiKeyId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `toodledo_id` (`toodledo_id`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_ToodledoTask`
--

DROP TABLE IF EXISTS `Facet_ToodledoTask`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_ToodledoTask` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `_length` int(11) NOT NULL,
  `_order` int(11) NOT NULL,
  `_priority` tinyint(4) NOT NULL,
  `_repeat` varchar(255) DEFAULT NULL,
  `_timer` bigint(20) NOT NULL,
  `added` bigint(20) NOT NULL,
  `children` int(11) NOT NULL,
  `completed` bigint(20) NOT NULL,
  `context` bigint(20) NOT NULL,
  `duedate` bigint(20) NOT NULL,
  `duedatemod` tinyint(4) NOT NULL,
  `duetime` bigint(20) NOT NULL,
  `folder` bigint(20) NOT NULL,
  `goal` bigint(20) NOT NULL,
  `location` bigint(20) NOT NULL,
  `meta` varchar(255) DEFAULT NULL,
  `modified` bigint(20) NOT NULL,
  `note` longtext,
  `parent` bigint(20) NOT NULL,
  `remind` int(11) NOT NULL,
  `repeatfrom` int(11) NOT NULL,
  `star` tinyint(4) NOT NULL,
  `startdate` bigint(20) NOT NULL,
  `starttime` bigint(20) NOT NULL,
  `status` tinyint(4) NOT NULL,
  `tag` varchar(255) DEFAULT NULL,
  `timeron` bigint(20) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `toodledo_id` bigint(20) NOT NULL,
  `tags` longtext,
  `apiKeyId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `toodledo_id` (`toodledo_id`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_Tweet`
--

DROP TABLE IF EXISTS `Facet_Tweet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_Tweet` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `profileImageUrl` varchar(255) DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `tweetId` bigint(20) NOT NULL,
  `userName` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=575 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_TwitterDirectMessage`
--

DROP TABLE IF EXISTS `Facet_TwitterDirectMessage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_TwitterDirectMessage` (
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
  `recipientName` varchar(255) DEFAULT NULL,
  `recipientProfileImageUrl` varchar(255) DEFAULT NULL,
  `recipientScreenName` varchar(255) DEFAULT NULL,
  `senderName` varchar(255) DEFAULT NULL,
  `senderProfileImageUrl` varchar(255) DEFAULT NULL,
  `senderScreenName` varchar(255) DEFAULT NULL,
  `sent` tinyint(4) NOT NULL,
  `text` varchar(255) DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `twitterId` bigint(20) NOT NULL,
  `apiKeyId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `sent_index` (`sent`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_TwitterMention`
--

DROP TABLE IF EXISTS `Facet_TwitterMention`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_TwitterMention` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `profileImageUrl` varchar(255) DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `twitterId` bigint(20) NOT NULL,
  `userName` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=368 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_WithingsBPMMeasure`
--

DROP TABLE IF EXISTS `Facet_WithingsBPMMeasure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_WithingsBPMMeasure` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `diastolic` float NOT NULL,
  `heartPulse` float NOT NULL,
  `measureTime` bigint(20) NOT NULL,
  `systolic` float NOT NULL,
  `tags` longtext,
  `apiKeyId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=2832 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_WithingsBodyScaleMeasure`
--

DROP TABLE IF EXISTS `Facet_WithingsBodyScaleMeasure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_WithingsBodyScaleMeasure` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `fatFreeMass` float NOT NULL,
  `fatMassWeight` float NOT NULL,
  `fatRatio` float NOT NULL,
  `height` float NOT NULL,
  `measureTime` bigint(20) NOT NULL,
  `weight` float NOT NULL,
  `tags` longtext,
  `apiKeyId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=6330 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_ZeoSleepStats`
--

DROP TABLE IF EXISTS `Facet_ZeoSleepStats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_ZeoSleepStats` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `awakenings` int(11) NOT NULL,
  `morningFeel` int(11) NOT NULL,
  `sleepGraph` longtext,
  `timeInDeepPercentage` int(11) NOT NULL,
  `timeInLightPercentage` int(11) NOT NULL,
  `timeInRemPercentage` int(11) NOT NULL,
  `timeInWakePercentage` int(11) NOT NULL,
  `timeToZ` int(11) NOT NULL,
  `totalZ` int(11) NOT NULL,
  `zq` int(11) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=3618 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FitbitUserProfile`
--

DROP TABLE IF EXISTS `FitbitUserProfile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FitbitUserProfile` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `firstSeenHere` bigint(20) NOT NULL,
  `guestId` bigint(20) NOT NULL,
  `aboutMe` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `dateOfBirth` varchar(255) DEFAULT NULL,
  `displayName` varchar(255) DEFAULT NULL,
  `encodedId` varchar(255) DEFAULT NULL,
  `fullName` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `height` double NOT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `offsetFromUTCMillis` bigint(20) NOT NULL,
  `state` varchar(255) DEFAULT NULL,
  `strideLengthRunning` double NOT NULL,
  `strideLengthWalking` double NOT NULL,
  `timezone` varchar(255) DEFAULT NULL,
  `weight` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=35 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FitnessActivityDistance`
--

DROP TABLE IF EXISTS `FitnessActivityDistance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FitnessActivityDistance` (
  `FitnessActivityID` bigint(20) NOT NULL,
  `distance` double NOT NULL,
  `timestamp` double NOT NULL,
  KEY `FKBCE560C6B3E8E7` (`FitnessActivityID`),
  CONSTRAINT `FKBCE560C6B3E8E7` FOREIGN KEY (`FitnessActivityID`) REFERENCES `Facet_RunKeeperFitnessActivity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FitnessActivityHeartRate`
--

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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GrapherView`
--

DROP TABLE IF EXISTS `GrapherView`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GrapherView` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guestId` bigint(20) NOT NULL,
  `json` longtext,
  `lastUsed` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `name` (`name`),
  KEY `guestId` (`guestId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Guest`
--

DROP TABLE IF EXISTS `Guest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Guest` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `firstname` varchar(255) DEFAULT NULL,
  `lastname` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `roles` varchar(255) DEFAULT NULL,
  `salt` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `email_index` (`email`),
  KEY `username_index` (`username`)
) ENGINE=MyISAM AUTO_INCREMENT=43 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Notifications`
--

DROP TABLE IF EXISTS `Notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Notifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `deleted` char(1) NOT NULL,
  `guestId` bigint(20) NOT NULL,
  `message` longtext,
  `ts` bigint(20) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `repeated` smallint(6) NOT NULL DEFAULT '1',
  `stackTrace` longtext,
  PRIMARY KEY (`id`),
  KEY `guestId_index` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=237 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ResetPasswordToken`
--

DROP TABLE IF EXISTS `ResetPasswordToken`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ResetPasswordToken` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guestId` bigint(20) NOT NULL,
  `token` varchar(255) DEFAULT NULL,
  `ts` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ScheduledUpdate`
--

DROP TABLE IF EXISTS `ScheduledUpdate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ScheduledUpdate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auditTrail` longtext,
  `connectorName` varchar(255) DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `jsonParams` varchar(255) DEFAULT NULL,
  `objectTypes` int(11) NOT NULL,
  `retries` int(11) NOT NULL,
  `status` int(11) DEFAULT NULL,
  `timeScheduled` bigint(20) NOT NULL,
  `updateType` int(11) DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `serverUUID` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=14779 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Settings`
--

DROP TABLE IF EXISTS `Settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Settings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `distanceMeasureUnit` int(11) DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `lengthMeasureUnit` int(11) DEFAULT NULL,
  `temperatureUnit` int(11) DEFAULT NULL,
  `weightMeasureUnit` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=21 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SharedConnectors`
--

DROP TABLE IF EXISTS `SharedConnectors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SharedConnectors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `connectorName` varchar(255) NOT NULL,
  `filterJson` longtext,
  `buddy_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_BUDDY` (`buddy_id`),
  KEY `connectorName` (`connectorName`),
  CONSTRAINT `FK_BUDDY` FOREIGN KEY (`buddy_id`) REFERENCES `CoachingBuddies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Tags`
--

DROP TABLE IF EXISTS `Tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Tags` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guestId` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UPC`
--

DROP TABLE IF EXISTS `UPC`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UPC` (
  `ID` varchar(255) NOT NULL,
  `productName` varchar(255) DEFAULT NULL,
  `productDescription` text,
  `brand` varchar(255) DEFAULT NULL,
  `manufacturer` varchar(255) DEFAULT NULL,
  `productSize` float DEFAULT NULL,
  `productSizeUOM` varchar(40) DEFAULT NULL,
  `SIproductSize` float DEFAULT NULL,
  `SIproductSizeUOM` varchar(10) DEFAULT NULL,
  `servingSize` float DEFAULT NULL,
  `servingSizeUOM` varchar(40) DEFAULT NULL,
  `SIservingSize` float DEFAULT NULL,
  `SIservingSizeUOM` varchar(10) DEFAULT NULL,
  `ingredients` text,
  `PLU` varchar(15) NOT NULL,
  `UPC_E` varchar(15) NOT NULL,
  `UPC_A` varchar(15) NOT NULL,
  `calories` float DEFAULT NULL,
  `gramsOfFat` float DEFAULT NULL,
  `gramsOfSatFat` float DEFAULT NULL,
  `gramsOfTransFat` float DEFAULT NULL,
  `gramsOfCarbs` float DEFAULT NULL,
  `milligramsOfSodium` float DEFAULT NULL,
  `gramsOfFiber` float DEFAULT NULL,
  `gramsOfProtein` float DEFAULT NULL,
  `gramsOfSugar` float DEFAULT NULL,
  `milligramsOfCholesterol` float DEFAULT NULL,
  `fatCaloriesPerServing` float DEFAULT NULL,
  `servingsPerContainer` float DEFAULT NULL,
  `sugarPerServing` float DEFAULT NULL,
  `healthScore` float DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WeatherInfo`
--

DROP TABLE IF EXISTS `WeatherInfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WeatherInfo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `city` varchar(255) DEFAULT NULL,
  `cloudcover` int(11) NOT NULL,
  `fdate` varchar(255) DEFAULT NULL,
  `humidity` int(11) NOT NULL,
  `minuteOfDay` int(11) NOT NULL,
  `precipMM` float NOT NULL,
  `pressure` int(11) NOT NULL,
  `tempC` int(11) NOT NULL,
  `tempF` int(11) NOT NULL,
  `visibility` int(11) NOT NULL,
  `weatherCode` int(11) NOT NULL,
  `weatherDesc` varchar(255) DEFAULT NULL,
  `weatherIconUrl` varchar(255) DEFAULT NULL,
  `weatherIconUrlDay` varchar(255) DEFAULT NULL,
  `weatherIconUrlNight` varchar(255) DEFAULT NULL,
  `winddir16Point` varchar(255) DEFAULT NULL,
  `winddirDegree` int(11) NOT NULL,
  `windspeedKmph` int(11) NOT NULL,
  `windspeedMiles` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `city_index` (`city`),
  KEY `fdate_index` (`fdate`),
  KEY `minuteOfDay_index` (`minuteOfDay`)
) ENGINE=MyISAM AUTO_INCREMENT=46527 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WidgetSettings`
--

DROP TABLE IF EXISTS `WidgetSettings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WidgetSettings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dashboardId` bigint(20) NOT NULL,
  `guestId` bigint(20) NOT NULL,
  `settingsJSON` longtext,
  `widgetName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `widgetName` (`widgetName`),
  KEY `dashboardId` (`dashboardId`),
  KEY `guest_index` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cities1000`
--

DROP TABLE IF EXISTS `cities1000`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cities1000` (
  `geo_id` int(11) unsigned NOT NULL,
  `geo_name` varchar(200) NOT NULL DEFAULT '',
  `geo_ansiname` varchar(200) NOT NULL DEFAULT '',
  `geo_alternate_names` varchar(2000) NOT NULL DEFAULT '',
  `geo_latitude` double(11,7) NOT NULL DEFAULT '0.0000000',
  `geo_longitude` double(11,7) NOT NULL DEFAULT '0.0000000',
  `geo_feature_class` char(1) DEFAULT NULL,
  `geo_feature_code` varchar(10) DEFAULT NULL,
  `geo_country_code` char(2) DEFAULT NULL,
  `geo_country_code2` varchar(60) DEFAULT NULL,
  `geo_admin1_code` varchar(20) DEFAULT '',
  `geo_admin2_code` varchar(80) DEFAULT '',
  `geo_admin3_code` varchar(20) DEFAULT '',
  `geo_admin4_code` varchar(20) DEFAULT '',
  `geo_population` bigint(11) DEFAULT '0',
  `geo_elevation` int(11) DEFAULT '0',
  `geo_gtopo30` int(11) DEFAULT '0',
  `geo_timezone` varchar(40) DEFAULT NULL,
  `geo_mod_date` date DEFAULT '0000-00-00',
  `population` bigint(20) NOT NULL,
  PRIMARY KEY (`geo_id`),
  KEY `geo_latitude` (`geo_latitude`),
  KEY `geo_longitude` (`geo_longitude`),
  KEY `name` (`geo_name`),
  FULLTEXT KEY `geo_name` (`geo_name`),
  FULLTEXT KEY `geo_alternate_names` (`geo_alternate_names`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-03-07 13:43:28
