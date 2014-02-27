-- MySQL dump 10.13  Distrib 5.6.12, for osx10.7 (x86_64)
--
-- Host: localhost    Database: flx
-- ------------------------------------------------------
-- Server version	5.6.12

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
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `jsonStorage` longtext COLLATE utf8mb4_unicode_ci,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `radius` double NOT NULL,
  `since` bigint(20) NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `until` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `settingsStorage` longblob,
  `status` tinyint(4) DEFAULT NULL,
  `stackTrace` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `api_index` (`api`),
  KEY `guestId_index` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=98 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ApiKeyAttribute`
--

DROP TABLE IF EXISTS `ApiKeyAttribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ApiKeyAttribute` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `attributeKey` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attributeValue` longtext COLLATE utf8mb4_unicode_ci,
  `apiKey_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK63B21617FB39AB73` (`apiKey_id`)
) ENGINE=MyISAM AUTO_INCREMENT=71292 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `content` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `ts` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ts` (`ts`),
  KEY `guestId` (`guestId`),
  KEY `api` (`api`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `query` longtext COLLATE utf8mb4_unicode_ci,
  `success` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ts` bigint(20) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `httpResponseCode` int(11) DEFAULT NULL,
  `reason` mediumtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `elapsed` (`elapsed`),
  KEY `ts` (`ts`),
  KEY `objectTypes` (`objectTypes`),
  KEY `guestId` (`guestId`),
  KEY `api` (`api`),
  KEY `success` (`success`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `httpResponseCode` (`httpResponseCode`)
) ENGINE=MyISAM AUTO_INCREMENT=2243037 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ChannelMapping`
--

DROP TABLE IF EXISTS `ChannelMapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ChannelMapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `channelName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channelType` int(11) DEFAULT NULL,
  `deviceName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `guestId` bigint(20) DEFAULT NULL,
  `internalChannelName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `internalDeviceName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `objectTypeId` int(11) DEFAULT NULL,
  `timeType` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `channelName` (`channelName`(191)),
  KEY `deviceName` (`deviceName`(191)),
  KEY `guestId` (`guestId`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ChannelStyle`
--

DROP TABLE IF EXISTS `ChannelStyle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ChannelStyle` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `channelName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deviceName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `json` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`),
  KEY `channelName` (`channelName`(250)),
  KEY `deviceName` (`deviceName`(250))
) ENGINE=MyISAM AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `connectUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `connectorName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `count` int(11) NOT NULL,
  `enabled` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manageable` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `text` longtext COLLATE utf8mb4_unicode_ci,
  `supportsRenewTokens` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `renewTokensUrlTemplate` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `supportsSync` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `supportsFileUpload` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `apiKeyAttributeKeys` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=3520 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `channels` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`),
  KEY `api` (`api`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `stateJSON` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Dashboard`
--

DROP TABLE IF EXISTS `Dashboard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Dashboard` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `active` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `guestId` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ordering` int(11) NOT NULL,
  `widgetNames` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `json` longtext COLLATE utf8mb4_unicode_ci,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastSync` bigint(20) NOT NULL,
  `estimatedCalories` int(11) NOT NULL,
  `predictedCalories` int(11) NOT NULL,
  `totalCalories` int(11) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`),
  KEY `date` (`date`(250))
) ENGINE=MyISAM AUTO_INCREMENT=2567 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `json` longtext COLLATE utf8mb4_unicode_ci,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastSync` bigint(20) NOT NULL,
  `efficiency` double NOT NULL,
  `totalLying` int(11) NOT NULL,
  `totalSleeping` int(11) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`),
  KEY `date` (`date`(250))
) ENGINE=MyISAM AUTO_INCREMENT=1129 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `json` longtext COLLATE utf8mb4_unicode_ci,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastSync` bigint(20) NOT NULL,
  `totalSteps` int(11) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
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
) ENGINE=MyISAM AUTO_INCREMENT=1809 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `edited` bigint(20) NOT NULL,
  `entryId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icalUID` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `kind` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `linkHref` longtext COLLATE utf8mb4_unicode_ci,
  `linkTitle` longtext COLLATE utf8mb4_unicode_ci,
  `locationsStorage` longblob,
  `participantsStorage` longblob,
  `plainTextContent` longtext COLLATE utf8mb4_unicode_ci,
  `published` bigint(20) NOT NULL,
  `summary` longtext COLLATE utf8mb4_unicode_ci,
  `textContent` longtext COLLATE utf8mb4_unicode_ci,
  `title` longtext COLLATE utf8mb4_unicode_ci,
  `whenStorage` longblob,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
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
) ENGINE=MyISAM AUTO_INCREMENT=335 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `callType` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `emailId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `personName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `personNumber` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `seconds` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `activeScore` int(11) NOT NULL,
  `activityCalories` int(11) NOT NULL,
  `caloriesJson` longtext COLLATE utf8mb4_unicode_ci,
  `caloriesOut` int(11) NOT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
  `stepsJson` longtext COLLATE utf8mb4_unicode_ci,
  `totalDistance` double NOT NULL,
  `trackerDistance` double NOT NULL,
  `veryActiveDistance` double NOT NULL,
  `veryActiveMinutes` int(11) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`),
  KEY `date` (`date`(250))
) ENGINE=MyISAM AUTO_INCREMENT=6322 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`),
  KEY `date` (`date`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `awakeningsCount` int(11) NOT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isMainSleep` bit(1) NOT NULL,
  `logId` bigint(20) NOT NULL,
  `minutesAfterWakeup` int(11) NOT NULL,
  `minutesAsleep` int(11) NOT NULL,
  `minutesAwake` int(11) NOT NULL,
  `minutesToFallAsleep` int(11) NOT NULL,
  `timeInBed` int(11) NOT NULL,
  `duration` int(11) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`),
  KEY `date` (`date`(250))
) ENGINE=MyISAM AUTO_INCREMENT=5506 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `bmi` double NOT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fat` double NOT NULL,
  `weight` double NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`),
  KEY `date` (`date`(191))
) ENGINE=InnoDB AUTO_INCREMENT=5441 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `accuracy` int(11) NOT NULL,
  `datetaken` bigint(20) NOT NULL,
  `dateupload` bigint(20) NOT NULL,
  `farm` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flickrId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isfamily` bit(1) NOT NULL,
  `isfriend` bit(1) NOT NULL,
  `ispublic` bit(1) NOT NULL,
  `latitude` float DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  `owner` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `secret` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `server` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dateupdated` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`),
  KEY `date` (`date`(250))
) ENGINE=MyISAM AUTO_INCREMENT=9635 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `start` bigint(20) NOT NULL,
  `end` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `captureYYYYDDD` char(7) COLLATE utf8mb4_unicode_ci NOT NULL,
  `imageType` char(3) COLLATE utf8mb4_unicode_ci NOT NULL,
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
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `heading` float DEFAULT NULL,
  `headingRef` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `altitude` float DEFAULT NULL,
  `altitudeRef` int(11) DEFAULT NULL,
  `gpsPrecision` float DEFAULT NULL,
  `gpsDatestamp` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gpsTimestamp` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `api_index` (`api`),
  KEY `end_index` (`end`),
  KEY `guestId_index` (`guestId`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `objectType_index` (`objectType`),
  KEY `start_index` (`start`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `hash_index` (`hash`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=876 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_GoogleCalendarEvent`
--

DROP TABLE IF EXISTS `Facet_GoogleCalendarEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_GoogleCalendarEvent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `attendeesStorage` longtext,
  `calendarId` varchar(255) DEFAULT NULL,
  `colorId` longtext,
  `created` bigint(20) NOT NULL,
  `eventUpdated` bigint(20) NOT NULL,
  `creatorStorage` longtext,
  `description` longtext,
  `endTimeUnspecified` bit(1) DEFAULT NULL,
  `endTimezoneShift` int(11) NOT NULL,
  `etag` longtext,
  `googleId` varchar(255) DEFAULT NULL,
  `guestsCanSeeOtherGuests` bit(1) DEFAULT NULL,
  `hangoutLink` longtext,
  `htmlLink` longtext,
  `iCalUID` longtext,
  `kind` longtext,
  `location` longtext,
  `locked` bit(1) DEFAULT NULL,
  `organizerStorage` longtext,
  `originalStartTime` bigint(20) NOT NULL,
  `recurrence` longtext,
  `recurringEventId` varchar(255) DEFAULT NULL,
  `sequence` int(11) DEFAULT NULL,
  `startTimezoneShift` int(11) NOT NULL,
  `status` longtext,
  `summary` longtext,
  `transparency` varchar(255) DEFAULT NULL,
  `visibility` varchar(255) DEFAULT NULL,
  `allDayEvent` bit(1) DEFAULT NULL,
  `startDate` date DEFAULT NULL,
  `endDate` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `update_index` (`eventUpdated`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `googleId` (`googleId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=12563 DEFAULT CHARSET=utf8;
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
  `url` mediumtext,
  `tags` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `mbid` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=MyISAM AUTO_INCREMENT=91702 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_Location`
--

DROP TABLE IF EXISTS `Facet_Location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_Location` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `accuracy` int(11) NOT NULL,
  `altitude` int(11) NOT NULL,
  `altitudeAccuracy` int(11) NOT NULL,
  `device` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `heading` int(11) NOT NULL,
  `latitude` float NOT NULL,
  `longitude` float NOT NULL,
  `os` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `placeid` int(11) NOT NULL,
  `source` int(11) DEFAULT NULL,
  `speed` int(11) NOT NULL,
  `timestampMs` bigint(20) NOT NULL,
  `version` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `date` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timezone` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timezoneMinutesOffset` int(11) DEFAULT NULL,
  `uri` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `processed` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
  KEY `uri` (`uri`(250)),
  KEY `apiKey` (`apiKeyId`),
  KEY `processed_index` (`processed`)
) ENGINE=MyISAM AUTO_INCREMENT=2862391 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_MovesMove`
--

DROP TABLE IF EXISTS `Facet_MovesMove`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_MovesMove` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `date` (`date`(191))
) ENGINE=InnoDB AUTO_INCREMENT=2348 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_MovesPlace`
--

DROP TABLE IF EXISTS `Facet_MovesPlace`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_MovesPlace` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `foursquareId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` float NOT NULL,
  `longitude` float NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `placeId` bigint(20) DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `date` (`date`(191))
) ENGINE=InnoDB AUTO_INCREMENT=2650 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `note` longtext COLLATE utf8mb4_unicode_ci,
  `mymeeId` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timezoneOffset` int(11) NOT NULL,
  `amount` double DEFAULT NULL,
  `baseAmount` int(11) DEFAULT NULL,
  `unit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `baseUnit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `imageURL` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `name` (`name`(191)),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `result_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_value` double NOT NULL,
  `session_timestamp` bigint(20) NOT NULL,
  `test_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
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
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `comments` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration` int(11) NOT NULL,
  `equipment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_live` bit(1) NOT NULL,
  `source` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_climb` double NOT NULL,
  `total_distance` double NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `uri` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `userID` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timeZone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=455 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `dateReceived` datetime DEFAULT NULL,
  `emailId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` longtext COLLATE utf8mb4_unicode_ci,
  `personName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `personNumber` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `smsType` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `profileImageUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `tweetId` bigint(20) NOT NULL,
  `userName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
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
) ENGINE=MyISAM AUTO_INCREMENT=1746 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `recipientName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recipientProfileImageUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recipientScreenName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `senderName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `senderProfileImageUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `senderScreenName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sent` tinyint(4) NOT NULL,
  `text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profileImageUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `twitterId` bigint(20) NOT NULL,
  `userName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
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
) ENGINE=MyISAM AUTO_INCREMENT=612 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_VisitedCity`
--

DROP TABLE IF EXISTS `Facet_VisitedCity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_VisitedCity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locationSource` int(11) DEFAULT NULL,
  `count` int(11) NOT NULL,
  `sunrise` int(11) NOT NULL,
  `sunset` int(11) NOT NULL,
  `city_geo_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `locationSource_index` (`locationSource`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `FK68B7FCEDE76FB50E` (`city_geo_id`),
  KEY `date` (`date`(250))
) ENGINE=MyISAM AUTO_INCREMENT=6227 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `diastolic` float NOT NULL,
  `heartPulse` float NOT NULL,
  `measureTime` bigint(20) NOT NULL,
  `systolic` float NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
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
) ENGINE=MyISAM AUTO_INCREMENT=2489 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `fatFreeMass` float NOT NULL,
  `fatMassWeight` float NOT NULL,
  `fatRatio` float NOT NULL,
  `height` float NOT NULL,
  `measureTime` bigint(20) NOT NULL,
  `weight` float NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
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
) ENGINE=MyISAM AUTO_INCREMENT=3337 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_WithingsHeartPulseMeasure`
--

DROP TABLE IF EXISTS `Facet_WithingsHeartPulseMeasure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_WithingsHeartPulseMeasure` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `heartPulse` float NOT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=214 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `timeUpdated` bigint(20) NOT NULL,
  `awakenings` int(11) NOT NULL,
  `morningFeel` int(11) NOT NULL,
  `sleepGraph` longtext COLLATE utf8mb4_unicode_ci,
  `timeInDeepPercentage` int(11) NOT NULL,
  `timeInLightPercentage` int(11) NOT NULL,
  `timeInRemPercentage` int(11) NOT NULL,
  `timeInWakePercentage` int(11) NOT NULL,
  `timeToZ` int(11) NOT NULL,
  `totalZ` int(11) NOT NULL,
  `zq` int(11) NOT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKey` (`apiKeyId`),
  KEY `date` (`date`(250))
) ENGINE=MyISAM AUTO_INCREMENT=2116 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FoursquareVenue`
--

DROP TABLE IF EXISTS `FoursquareVenue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FoursquareVenue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `canonicalUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categoryFoursquareId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categoryIconUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categoryName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categoryShortName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `foursquareId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categoryIconUrlPrefix` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categoryIconUrlSuffix` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `foursquareId` (`foursquareId`(191))
) ENGINE=InnoDB AUTO_INCREMENT=95 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `json` longtext COLLATE utf8mb4_unicode_ci,
  `lastUsed` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `name` (`name`(250)),
  KEY `guestId` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Guest`
--

DROP TABLE IF EXISTS `Guest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Guest` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `firstname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lastname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `roles` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `salt` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tourState` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `autoLoginTokenTimestamp` bigint(21) DEFAULT NULL,
  `autoLoginToken` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `registrationMethod` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `email_index` (`email`(250)),
  KEY `username_index` (`username`(250))
) ENGINE=MyISAM AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `MovesMoveActivity`
--

DROP TABLE IF EXISTS `MovesMoveActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `MovesMoveActivity` (
  `ActivityID` bigint(20) NOT NULL,
  `activity` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activityURI` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `distance` int(11) NOT NULL,
  `end` bigint(20) NOT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start` bigint(20) NOT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `steps` int(11) DEFAULT NULL,
  KEY `FK576EF1C2185495D1` (`ActivityID`),
  CONSTRAINT `FK576EF1C2185495D1` FOREIGN KEY (`ActivityID`) REFERENCES `Facet_MovesMove` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `MovesPlaceActivity`
--

DROP TABLE IF EXISTS `MovesPlaceActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `MovesPlaceActivity` (
  `ActivityID` bigint(20) NOT NULL,
  `activity` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activityURI` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `distance` int(11) NOT NULL,
  `end` bigint(20) NOT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start` bigint(20) NOT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `steps` int(11) DEFAULT NULL,
  KEY `FKC80F71B4738B879` (`ActivityID`),
  CONSTRAINT `FKC80F71B4738B879` FOREIGN KEY (`ActivityID`) REFERENCES `Facet_MovesPlace` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Notifications`
--

DROP TABLE IF EXISTS `Notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Notifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `deleted` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `guestId` bigint(20) NOT NULL,
  `message` longtext COLLATE utf8mb4_unicode_ci,
  `ts` bigint(20) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `repeated` smallint(6) NOT NULL DEFAULT '1',
  `stackTrace` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `guestId_index` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=123 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ts` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `messageDisplayCountersStorage` mediumtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SharedConnectors`
--

DROP TABLE IF EXISTS `SharedConnectors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SharedConnectors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `connectorName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `filterJson` longtext COLLATE utf8mb4_unicode_ci,
  `buddy_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_BUDDY` (`buddy_id`),
  KEY `connectorName` (`connectorName`(191)),
  CONSTRAINT `FK_BUDDY` FOREIGN KEY (`buddy_id`) REFERENCES `CoachingBuddies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `name` (`name`(191))
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UpdateWorkerTask`
--

DROP TABLE IF EXISTS `UpdateWorkerTask`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UpdateWorkerTask` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auditTrail` longtext COLLATE utf8mb4_unicode_ci,
  `connectorName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `jsonParams` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `objectTypes` int(11) NOT NULL,
  `retries` int(11) NOT NULL,
  `status` int(11) DEFAULT NULL,
  `timeScheduled` bigint(20) NOT NULL,
  `updateType` int(11) DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `serverUUID` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `status_index` (`status`),
  KEY `serverUUID_index` (`serverUUID`),
  KEY `apiKeyId_index` (`apiKeyId`),
  KEY `connectorName_index` (`connectorName`(250)),
  KEY `timeScheduled_index` (`timeScheduled`),
  KEY `updateType_index` (`updateType`),
  KEY `guestId_index` (`guestId`),
  KEY `objectTypes_index` (`objectTypes`),
  KEY `retries_index` (`retries`)
) ENGINE=MyISAM AUTO_INCREMENT=1771732 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WeatherInfo`
--

DROP TABLE IF EXISTS `WeatherInfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WeatherInfo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cloudcover` int(11) NOT NULL,
  `fdate` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `humidity` int(11) NOT NULL,
  `minuteOfDay` int(11) NOT NULL,
  `precipMM` float NOT NULL,
  `pressure` int(11) NOT NULL,
  `tempC` int(11) NOT NULL,
  `tempF` int(11) NOT NULL,
  `visibility` int(11) NOT NULL,
  `weatherCode` int(11) NOT NULL,
  `weatherDesc` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weatherIconUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weatherIconUrlDay` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weatherIconUrlNight` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `winddir16Point` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `winddirDegree` int(11) NOT NULL,
  `windspeedKmph` int(11) NOT NULL,
  `windspeedMiles` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `city_index` (`city`(250)),
  KEY `fdate_index` (`fdate`(250)),
  KEY `minuteOfDay_index` (`minuteOfDay`)
) ENGINE=MyISAM AUTO_INCREMENT=77334 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `settingsJSON` longtext COLLATE utf8mb4_unicode_ci,
  `widgetName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `widgetName` (`widgetName`(250)),
  KEY `dashboardId` (`dashboardId`),
  KEY `guest_index` (`guestId`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cities1000`
--

DROP TABLE IF EXISTS `cities1000`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cities1000` (
  `geo_id` int(11) unsigned NOT NULL,
  `geo_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `geo_ansiname` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `geo_alternate_names` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `geo_latitude` double(11,7) NOT NULL DEFAULT '0.0000000',
  `geo_longitude` double(11,7) NOT NULL DEFAULT '0.0000000',
  `geo_feature_class` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `geo_feature_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `geo_country_code` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `geo_country_code2` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `geo_admin1_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `geo_admin2_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `geo_admin3_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `geo_admin4_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `geo_population` bigint(11) DEFAULT '0',
  `geo_elevation` int(11) DEFAULT '0',
  `geo_gtopo30` int(11) DEFAULT '0',
  `geo_timezone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `geo_mod_date` date DEFAULT '0000-00-00',
  `population` bigint(20) NOT NULL,
  PRIMARY KEY (`geo_id`),
  KEY `geo_latitude` (`geo_latitude`),
  KEY `geo_longitude` (`geo_longitude`),
  KEY `name` (`geo_name`),
  FULLTEXT KEY `geo_name` (`geo_name`),
  FULLTEXT KEY `geo_alternate_names` (`geo_alternate_names`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-09-27 10:54:20
