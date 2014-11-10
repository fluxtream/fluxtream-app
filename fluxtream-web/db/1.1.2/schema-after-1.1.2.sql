-- MySQL dump 10.13  Distrib 5.5.29, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: flx
-- ------------------------------------------------------
-- Server version	5.5.29-0ubuntu0.12.04.1

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
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8;
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
  `stackTrace` longtext,
  `synching` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `defaultSettingsStorage` longblob,
  `reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `api_index` (`api`),
  KEY `guestId_index` (`guestId`)
) ENGINE=InnoDB AUTO_INCREMENT=6045 DEFAULT CHARSET=utf8;
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
  KEY `FK63B21617FB39AB73` (`apiKey_id`),
  CONSTRAINT `FK63B21617FB39AB73` FOREIGN KEY (`apiKey_id`) REFERENCES `ApiKey` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14537453 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=12845 DEFAULT CHARSET=utf8;
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
  `query` longtext,
  `success` char(1) NOT NULL,
  `ts` bigint(20) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `httpResponseCode` int(11) DEFAULT NULL,
  `reason` mediumtext,
  PRIMARY KEY (`id`),
  KEY `elapsed` (`elapsed`),
  KEY `ts` (`ts`),
  KEY `objectTypes` (`objectTypes`),
  KEY `guestId` (`guestId`),
  KEY `api` (`api`),
  KEY `success` (`success`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `httpResponseCode` (`httpResponseCode`)
) ENGINE=InnoDB AUTO_INCREMENT=13777432 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Application`
--

DROP TABLE IF EXISTS `Application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Application` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` longtext,
  `guestId` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `sharedSecret` varchar(255) DEFAULT NULL,
  `uid` varchar(255) DEFAULT NULL,
  `organization` varchar(255) DEFAULT NULL,
  `registrationAllowed` char(1) NOT NULL,
  `addConnectorCallbackURL` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `uid` (`uid`),
  KEY `guestId` (`guestId`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `AuthorizationCode`
--

DROP TABLE IF EXISTS `AuthorizationCode`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `AuthorizationCode` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `applicationId` bigint(20) NOT NULL,
  `code` varchar(255) DEFAULT NULL,
  `creationTime` bigint(20) NOT NULL,
  `expirationTime` bigint(20) NOT NULL,
  `scopes` longtext,
  `state` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `applicationId` (`applicationId`),
  KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `AuthorizationCodeResponse`
--

DROP TABLE IF EXISTS `AuthorizationCodeResponse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `AuthorizationCodeResponse` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `authorizationCodeId` bigint(20) NOT NULL,
  `granted` char(1) NOT NULL,
  `guestId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `AuthorizationToken`
--

DROP TABLE IF EXISTS `AuthorizationToken`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `AuthorizationToken` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `accessToken` varchar(255) DEFAULT NULL,
  `authorizationCodeId` bigint(20) NOT NULL,
  `creationTime` bigint(20) NOT NULL,
  `expirationTime` bigint(20) NOT NULL,
  `refreshToken` varchar(255) DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `accessToken` (`accessToken`),
  KEY `refreshToken` (`refreshToken`),
  KEY `authorizationCodeId` (`authorizationCodeId`),
  KEY `guestId` (`guestId`)
) ENGINE=InnoDB AUTO_INCREMENT=229 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=355194 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  KEY `channelName` (`channelName`),
  KEY `deviceName` (`deviceName`),
  KEY `guestId` (`guestId`)
) ENGINE=InnoDB AUTO_INCREMENT=17657 DEFAULT CHARSET=utf8;
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
  KEY `buddyId_index` (`buddyId`)
) ENGINE=InnoDB AUTO_INCREMENT=160 DEFAULT CHARSET=utf8;
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
) ENGINE=MyISAM AUTO_INCREMENT=4023 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=219 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=1151 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
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
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=48066 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=2237 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DataUpdate`
--

DROP TABLE IF EXISTS `DataUpdate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DataUpdate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `additionalInfo` longtext,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `channelNames` longtext,
  `endTime` bigint(20) DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `objectTypeId` bigint(20) DEFAULT NULL,
  `startTime` bigint(20) DEFAULT NULL,
  `timestamp` bigint(20) NOT NULL,
  `type` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `GuestIdAndTimestamp` (`guestId`,`timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=1916763 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DatabaseMetadata`
--

DROP TABLE IF EXISTS `DatabaseMetadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DatabaseMetadata` (
  `version` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `json` longtext,
  `lastSync` bigint(20) NOT NULL,
  `estimatedCalories` int(11) NOT NULL,
  `predictedCalories` int(11) NOT NULL,
  `totalCalories` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=54662 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `json` longtext,
  `lastSync` bigint(20) NOT NULL,
  `efficiency` double NOT NULL,
  `totalLying` int(11) NOT NULL,
  `totalSleeping` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=43043 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `json` longtext,
  `lastSync` bigint(20) NOT NULL,
  `totalSteps` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=40139 DEFAULT CHARSET=utf8;
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
  `tags` longtext COLLATE utf8mb4_unicode_ci,
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=9316 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `emailId` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `emailId` (`emailId`)
) ENGINE=InnoDB AUTO_INCREMENT=117969 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_EvernoteNote`
--

DROP TABLE IF EXISTS `Facet_EvernoteNote`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_EvernoteNote` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `USN` int(11) DEFAULT NULL,
  `guid` varchar(127) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `altitude` double DEFAULT NULL,
  `author` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `contentClass` varchar(255) DEFAULT NULL,
  `contentHash` tinyblob,
  `contentLength` int(11) DEFAULT NULL,
  `created` bigint(20) DEFAULT NULL,
  `creatorId` int(11) DEFAULT NULL,
  `deleted` bigint(20) DEFAULT NULL,
  `htmlContent` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `lastEditedBy` longtext,
  `tagGuidsStorage` longtext,
  `lastEditorId` int(11) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `notebookGuid` varchar(255) DEFAULT NULL,
  `placeName` longtext,
  `reminderDoneTime` bigint(20) DEFAULT NULL,
  `reminderOrder` bigint(20) DEFAULT NULL,
  `reminderTime` bigint(20) DEFAULT NULL,
  `shareDate` bigint(20) DEFAULT NULL,
  `source` longtext,
  `sourceApplication` longtext,
  `sourceURL` longtext,
  `subjectDate` bigint(20) DEFAULT NULL,
  `title` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `updated` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `guid` (`guid`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=364693 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_EvernoteNotebook`
--

DROP TABLE IF EXISTS `Facet_EvernoteNotebook`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_EvernoteNotebook` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(127) DEFAULT NULL,
  `USN` int(11) DEFAULT NULL,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `defaultNotebook` bit(1) DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `published` bit(1) DEFAULT NULL,
  `serviceCreated` bigint(20) DEFAULT NULL,
  `serviceUpdated` bigint(20) DEFAULT NULL,
  `stack` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `publishingPublicDescription` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `publishingUri` varchar(255) DEFAULT NULL,
  `publishingNoteOrderValue` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `guid` (`guid`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=6600 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_EvernotePhoto`
--

DROP TABLE IF EXISTS `Facet_EvernotePhoto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_EvernotePhoto` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `USN` int(11) DEFAULT NULL,
  `guid` varchar(255) DEFAULT NULL,
  `resourceFacet_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3645693AFCFC4763` (`resourceFacet_id`),
  KEY `guid` (`guid`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=37599 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_EvernoteResource`
--

DROP TABLE IF EXISTS `Facet_EvernoteResource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_EvernoteResource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `USN` int(11) DEFAULT NULL,
  `guid` varchar(127) DEFAULT NULL,
  `alternateDataBodyHash` tinyblob,
  `alternateDataSize` int(11) DEFAULT NULL,
  `altitude` double DEFAULT NULL,
  `cameraMake` varchar(255) DEFAULT NULL,
  `cameraModel` varchar(255) DEFAULT NULL,
  `dataBodyHash` tinyblob,
  `dataSize` int(11) DEFAULT NULL,
  `fileName` text,
  `height` smallint(6) DEFAULT NULL,
  `isAttachment` bit(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `mime` varchar(255) DEFAULT NULL,
  `noteGuid` varchar(127) DEFAULT NULL,
  `recoType` varchar(255) DEFAULT NULL,
  `recognitionDataBodyHash` tinyblob,
  `recognitionDataSize` int(11) DEFAULT NULL,
  `sourceURL` text,
  `timestamp` bigint(20) DEFAULT NULL,
  `width` smallint(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `guid` (`guid`),
  KEY `noteGuid` (`noteGuid`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=690476 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_EvernoteTag`
--

DROP TABLE IF EXISTS `Facet_EvernoteTag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_EvernoteTag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guid` varchar(127) DEFAULT NULL,
  `USN` int(11) DEFAULT NULL,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `guid` (`guid`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=39769 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `activeScore` int(11) NOT NULL,
  `activityCalories` int(11) NOT NULL,
  `caloriesJson` longtext,
  `caloriesOut` int(11) NOT NULL,
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  `distanceJson` mediumtext,
  `floorsJson` mediumtext,
  `elevationJson` mediumtext,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=864278 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_FitbitFoodLogEntry`
--

DROP TABLE IF EXISTS `Facet_FitbitFoodLogEntry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_FitbitFoodLogEntry` (
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
  `date` varchar(255) DEFAULT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `NV_Calories` float NOT NULL,
  `NV_Carbs` float NOT NULL,
  `NV_Fat` float NOT NULL,
  `NV_Fiber` float NOT NULL,
  `NV_Protein` float NOT NULL,
  `NV_Sodium` float NOT NULL,
  `accessLevel` varchar(255) DEFAULT NULL,
  `amount` float NOT NULL,
  `brand` varchar(255) DEFAULT NULL,
  `calories` int(11) NOT NULL,
  `foodId` bigint(20) NOT NULL,
  `isFavorite` bit(1) NOT NULL,
  `locale` varchar(255) DEFAULT NULL,
  `logId` bigint(20) NOT NULL,
  `mealTypeId` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `unitId` int(11) NOT NULL,
  `unitName` varchar(255) DEFAULT NULL,
  `unitPlural` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `date` (`date`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_FitbitFoodLogSummary`
--

DROP TABLE IF EXISTS `Facet_FitbitFoodLogSummary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_FitbitFoodLogSummary` (
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
  `date` varchar(255) DEFAULT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `calories` float NOT NULL,
  `caloriesGoal` int(11) NOT NULL,
  `caloriesOutGoal` int(11) NOT NULL,
  `carbs` float NOT NULL,
  `fat` float NOT NULL,
  `fiber` float NOT NULL,
  `protein` float NOT NULL,
  `sodium` float NOT NULL,
  `water` float NOT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `date` (`date`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `activityId` bigint(20) NOT NULL,
  `activityParentId` bigint(20) NOT NULL,
  `calories` int(11) NOT NULL,
  `distance` double NOT NULL,
  `duration` int(11) NOT NULL,
  `isFavorite` bit(1) NOT NULL,
  `logId` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `steps` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=19525 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `awakeningsCount` int(11) NOT NULL,
  `duration` int(11) NOT NULL,
  `isMainSleep` bit(1) NOT NULL,
  `logId` bigint(20) NOT NULL,
  `minutesAfterWakeup` int(11) NOT NULL,
  `minutesAsleep` int(11) NOT NULL,
  `minutesAwake` int(11) NOT NULL,
  `minutesToFallAsleep` int(11) NOT NULL,
  `timeInBed` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=589671 DEFAULT CHARSET=utf8;
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
  `logId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=297381 DEFAULT CHARSET=utf8;
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
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  `dateupdated` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `date` (`date`(191))
) ENGINE=InnoDB AUTO_INCREMENT=14255418 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `thumbnail2` mediumblob,
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
) ENGINE=MyISAM AUTO_INCREMENT=25364 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `commitsJSON` longtext,
  `repoName` varchar(255) DEFAULT NULL,
  `repoURL` varchar(255) DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
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
  `fullTextDescription` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
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
  `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `endTimeUnspecified` bit(1) DEFAULT NULL,
  `endTimezoneShift` int(11) NOT NULL,
  `etag` longtext,
  `googleId` varchar(255) DEFAULT NULL,
  `guestsCanSeeOtherGuests` bit(1) DEFAULT NULL,
  `hangoutLink` longtext,
  `htmlLink` longtext,
  `iCalUID` longtext,
  `kind` longtext,
  `location` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `locked` bit(1) DEFAULT NULL,
  `organizerStorage` longtext,
  `originalStartTime` bigint(20) NOT NULL,
  `recurrence` longtext,
  `recurringEventId` varchar(255) DEFAULT NULL,
  `sequence` int(11) DEFAULT NULL,
  `startTimezoneShift` int(11) NOT NULL,
  `status` longtext,
  `summary` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
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
) ENGINE=InnoDB AUTO_INCREMENT=6085404 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_JawboneUpMeal`
--

DROP TABLE IF EXISTS `Facet_JawboneUpMeal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_JawboneUpMeal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `time_completed` bigint(20) DEFAULT NULL,
  `time_created` bigint(20) DEFAULT NULL,
  `time_updated` bigint(20) DEFAULT NULL,
  `tz` varchar(255) DEFAULT NULL,
  `xid` varchar(255) DEFAULT NULL,
  `place_acc` int(11) DEFAULT NULL,
  `place_lat` double DEFAULT NULL,
  `place_lon` double DEFAULT NULL,
  `place_name` varchar(255) DEFAULT NULL,
  `mealDetails` longtext,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `xid` (`xid`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `date` (`date`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=25680 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_JawboneUpMoves`
--

DROP TABLE IF EXISTS `Facet_JawboneUpMoves`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_JawboneUpMoves` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `time_completed` bigint(20) DEFAULT NULL,
  `time_created` bigint(20) DEFAULT NULL,
  `time_updated` bigint(20) DEFAULT NULL,
  `tz` varchar(255) DEFAULT NULL,
  `xid` varchar(255) DEFAULT NULL,
  `active_time` int(11) NOT NULL,
  `bg_calories` double NOT NULL,
  `bmr` double NOT NULL,
  `bmr_day` double NOT NULL,
  `calories` double NOT NULL,
  `distance` int(11) NOT NULL,
  `inactive_time` int(11) NOT NULL,
  `intensityStorage` longtext,
  `km` double NOT NULL,
  `longest_active` int(11) NOT NULL,
  `longest_idle` int(11) NOT NULL,
  `snapshot_image` varchar(255) DEFAULT NULL,
  `steps` int(11) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `tzs` mediumtext,
  `wo_active_time` int(11) NOT NULL,
  `wo_calories` double NOT NULL,
  `wo_count` int(11) NOT NULL,
  `wo_longest` int(11) NOT NULL,
  `wo_time` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `xid` (`xid`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `date` (`date`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=24717 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_JawboneUpServing`
--

DROP TABLE IF EXISTS `Facet_JawboneUpServing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_JawboneUpServing` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `time_completed` bigint(20) DEFAULT NULL,
  `time_created` bigint(20) DEFAULT NULL,
  `time_updated` bigint(20) DEFAULT NULL,
  `tz` varchar(255) DEFAULT NULL,
  `xid` varchar(255) DEFAULT NULL,
  `image` varchar(255) DEFAULT NULL,
  `servingDetails` longtext,
  `meal_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKC04FA357372A348` (`meal_id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `xid` (`xid`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `date` (`date`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  CONSTRAINT `FKC04FA357372A348` FOREIGN KEY (`meal_id`) REFERENCES `Facet_JawboneUpMeal` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=45646 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_JawboneUpSleep`
--

DROP TABLE IF EXISTS `Facet_JawboneUpSleep`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_JawboneUpSleep` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `time_completed` bigint(20) DEFAULT NULL,
  `time_created` bigint(20) DEFAULT NULL,
  `time_updated` bigint(20) DEFAULT NULL,
  `tz` varchar(255) DEFAULT NULL,
  `xid` varchar(255) DEFAULT NULL,
  `place_acc` int(11) DEFAULT NULL,
  `place_lat` double DEFAULT NULL,
  `place_lon` double DEFAULT NULL,
  `place_name` varchar(255) DEFAULT NULL,
  `asleep_time` bigint(20) NOT NULL,
  `awake` int(11) NOT NULL,
  `awake_time` bigint(20) NOT NULL,
  `awakenings` int(11) NOT NULL,
  `deep` int(11) NOT NULL,
  `duration` int(11) NOT NULL,
  `light` int(11) NOT NULL,
  `phasesStorage` longtext,
  `quality` int(11) NOT NULL,
  `rem` int(11) NOT NULL,
  `smart_alarm_fire` bigint(20) NOT NULL,
  `snapshot_image` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `xid` (`xid`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `date` (`date`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=17750 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_JawboneUpWorkout`
--

DROP TABLE IF EXISTS `Facet_JawboneUpWorkout`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_JawboneUpWorkout` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api` int(11) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `comment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `time_completed` bigint(20) DEFAULT NULL,
  `time_created` bigint(20) DEFAULT NULL,
  `time_updated` bigint(20) DEFAULT NULL,
  `tz` varchar(255) DEFAULT NULL,
  `xid` varchar(255) DEFAULT NULL,
  `place_acc` int(11) DEFAULT NULL,
  `place_lat` double DEFAULT NULL,
  `place_lon` double DEFAULT NULL,
  `place_name` varchar(255) DEFAULT NULL,
  `bg_active_time` int(11) DEFAULT NULL,
  `bg_calories` double DEFAULT NULL,
  `bmr` double DEFAULT NULL,
  `bmr_calories` double DEFAULT NULL,
  `calories` double DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `image` varchar(255) DEFAULT NULL,
  `intensity` int(11) DEFAULT NULL,
  `km` double DEFAULT NULL,
  `meters` double DEFAULT NULL,
  `route` varchar(255) DEFAULT NULL,
  `snapshot_image` varchar(255) DEFAULT NULL,
  `steps` int(11) DEFAULT NULL,
  `sub_type` int(11) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `workoutDetails` longtext,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `xid` (`xid`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `date` (`date`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`)
) ENGINE=InnoDB AUTO_INCREMENT=2574 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `album_mbid` varchar(255) DEFAULT NULL,
  `artist` varchar(255) DEFAULT NULL,
  `artist_mbid` varchar(255) DEFAULT NULL,
  `imgUrls` longtext,
  `name` varchar(255) DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=1463 DEFAULT CHARSET=latin1;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `album_mbid` varchar(255) DEFAULT NULL,
  `artist` varchar(255) DEFAULT NULL,
  `artist_mbid` varchar(255) DEFAULT NULL,
  `imgUrls` longtext,
  `name` varchar(255) DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `url` mediumtext,
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
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=9142902 DEFAULT CHARSET=utf8;
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
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  `uri` varchar(255) DEFAULT NULL,
  `processed` char(1) DEFAULT NULL,
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
  KEY `apiKeyId` (`apiKeyId`),
  KEY `uri` (`uri`),
  KEY `processed_index` (`processed`),
  KEY `source` (`source`),
  KEY `apiKeyIdEnd` (`apiKeyId`,`end`)
) ENGINE=InnoDB AUTO_INCREMENT=264929899 DEFAULT CHARSET=utf8;
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
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=1281236 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=1627039 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=118490 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `description` longtext,
  `photoId` varchar(255) DEFAULT NULL,
  `photoUrl` varchar(255) DEFAULT NULL,
  `thumbnailUrl` varchar(255) DEFAULT NULL,
  `thumbnailsJson` longtext,
  `title` varchar(255) DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=9537 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `result_name` varchar(255) DEFAULT NULL,
  `result_value` double NOT NULL,
  `session_timestamp` bigint(20) NOT NULL,
  `test_name` varchar(255) DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=12655 DEFAULT CHARSET=utf8;
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
  `distanceStorage` longtext,
  `heartRateStorage` longtext,
  `caloriesStorage` longtext,
  `totalCalories` double DEFAULT NULL,
  `averageHeartRate` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=85691 DEFAULT CHARSET=utf8;
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
  `emailId` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` longtext COLLATE utf8mb4_unicode_ci,
  `personName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `personNumber` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `smsType` int(11) DEFAULT NULL,
  `attachmentMimeTypes` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachmentNames` longtext COLLATE utf8mb4_unicode_ci,
  `hasAttachments` bit(1) DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `apiKey` (`apiKeyId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `emailId` (`emailId`)
) ENGINE=InnoDB AUTO_INCREMENT=587130 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `archived` tinyint(4) NOT NULL,
  `contributes` bigint(20) NOT NULL,
  `level` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `note` longtext,
  `toodledo_id` bigint(20) NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `toodledo_id` (`toodledo_id`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
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
  `tags` longtext,
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `toodledo_id` (`toodledo_id`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
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
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `profileImageUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `tweetId` bigint(20) NOT NULL,
  `userName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=831635 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `sent_index` (`sent`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=142895 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `timeUpdated` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profileImageUrl` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time` bigint(20) NOT NULL,
  `twitterId` bigint(20) NOT NULL,
  `userName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=358800 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
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
  KEY `date` (`date`)
) ENGINE=MyISAM AUTO_INCREMENT=2259940 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Facet_WithingsActivity`
--

DROP TABLE IF EXISTS `Facet_WithingsActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Facet_WithingsActivity` (
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
  `date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timezone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` longtext COLLATE utf8mb4_unicode_ci,
  `apiKeyId` bigint(20) DEFAULT NULL,
  `startTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `endTimeStorage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `steps` int(11) NOT NULL,
  `distance` float NOT NULL,
  `calories` float NOT NULL,
  `elevation` float NOT NULL,
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
) ENGINE=MyISAM AUTO_INCREMENT=19731 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `diastolic` float NOT NULL,
  `heartPulse` float NOT NULL,
  `measureTime` bigint(20) NOT NULL,
  `systolic` float NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=14214 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `fatFreeMass` float NOT NULL,
  `fatMassWeight` float NOT NULL,
  `fatRatio` float NOT NULL,
  `height` float NOT NULL,
  `measureTime` bigint(20) NOT NULL,
  `weight` float NOT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`)
) ENGINE=InnoDB AUTO_INCREMENT=119905 DEFAULT CHARSET=utf8;
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
  `comment` longtext,
  `end` bigint(20) NOT NULL,
  `fullTextDescription` longtext,
  `guestId` bigint(20) NOT NULL,
  `isEmpty` char(1) NOT NULL,
  `objectType` int(11) NOT NULL,
  `start` bigint(20) NOT NULL,
  `tags` longtext,
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
) ENGINE=InnoDB AUTO_INCREMENT=28642 DEFAULT CHARSET=utf8;
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
  `tags` longtext,
  `timeUpdated` bigint(20) NOT NULL,
  `date` varchar(255) DEFAULT NULL,
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
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `isEmpty_index` (`isEmpty`),
  KEY `end_index` (`end`),
  KEY `start_index` (`start`),
  KEY `api_index` (`api`),
  KEY `objectType_index` (`objectType`),
  KEY `guestId_index` (`guestId`),
  KEY `timeUpdated_index` (`timeUpdated`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=11844 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FitbitUserProfile`
--

DROP TABLE IF EXISTS `FitbitUserProfile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FitbitUserProfile` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
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
  `memberSince` varchar(20) DEFAULT NULL,
  `glucoseUnit` varchar(20) DEFAULT NULL,
  `heightUnit` varchar(20) DEFAULT NULL,
  `waterUnit` varchar(20) DEFAULT NULL,
  `weightUnit` varchar(20) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `avatar150` varchar(255) DEFAULT NULL,
  `startDayOfWeek` varchar(100) DEFAULT NULL,
  `apiKeyId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FoursquareVenue`
--

DROP TABLE IF EXISTS `FoursquareVenue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FoursquareVenue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `canonicalUrl` varchar(255) DEFAULT NULL,
  `categoryFoursquareId` varchar(255) DEFAULT NULL,
  `categoryIconUrl` varchar(255) DEFAULT NULL,
  `categoryName` varchar(255) DEFAULT NULL,
  `categoryShortName` varchar(255) DEFAULT NULL,
  `foursquareId` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `categoryIconUrlPrefix` varchar(255) DEFAULT NULL,
  `categoryIconUrlSuffix` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `foursquareId` (`foursquareId`)
) ENGINE=InnoDB AUTO_INCREMENT=16935 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=382 DEFAULT CHARSET=utf8;
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
  `registrationMethod` tinyint(4) DEFAULT NULL,
  `autoLoginToken` varchar(255) DEFAULT NULL,
  `autoLoginTokenTimestamp` bigint(21) DEFAULT NULL,
  `appId` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `email_index` (`email`),
  KEY `username_index` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2832 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `JawboneUpMovesHourlyTotals`
--

DROP TABLE IF EXISTS `JawboneUpMovesHourlyTotals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `JawboneUpMovesHourlyTotals` (
  `MovesRecordID` bigint(20) NOT NULL,
  `active_time` int(11) DEFAULT NULL,
  `calories` double DEFAULT NULL,
  `distance` int(11) DEFAULT NULL,
  `inactive_time` int(11) DEFAULT NULL,
  `longest_active_time` int(11) DEFAULT NULL,
  `longest_idle_time` int(11) DEFAULT NULL,
  `start` bigint(20) NOT NULL,
  `steps` int(11) DEFAULT NULL,
  KEY `FK2144A2A3AFFC9926` (`MovesRecordID`),
  CONSTRAINT `FK2144A2A3AFFC9926` FOREIGN KEY (`MovesRecordID`) REFERENCES `Facet_JawboneUpMoves` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `MovesMoveActivity`
--

DROP TABLE IF EXISTS `MovesMoveActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `MovesMoveActivity` (
  `ActivityID` bigint(20) NOT NULL,
  `activity` varchar(255) DEFAULT NULL,
  `activityURI` varchar(255) DEFAULT NULL,
  `date` varchar(255) DEFAULT NULL,
  `distance` int(11) NOT NULL,
  `end` bigint(20) NOT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `start` bigint(20) NOT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `steps` int(11) DEFAULT NULL,
  `activityGroup` varchar(255) DEFAULT NULL,
  `manual` bit(1) NOT NULL DEFAULT b'0',
  `duration` int(11) DEFAULT NULL,
  KEY `FK576EF1C2185495D1` (`ActivityID`),
  CONSTRAINT `FK576EF1C2185495D1` FOREIGN KEY (`ActivityID`) REFERENCES `Facet_MovesMove` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `MovesPlaceActivity`
--

DROP TABLE IF EXISTS `MovesPlaceActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `MovesPlaceActivity` (
  `ActivityID` bigint(20) NOT NULL,
  `activity` varchar(255) DEFAULT NULL,
  `activityURI` varchar(255) DEFAULT NULL,
  `date` varchar(255) DEFAULT NULL,
  `distance` int(11) NOT NULL,
  `end` bigint(20) NOT NULL,
  `endTimeStorage` varchar(255) DEFAULT NULL,
  `start` bigint(20) NOT NULL,
  `startTimeStorage` varchar(255) DEFAULT NULL,
  `steps` int(11) DEFAULT NULL,
  `activityGroup` varchar(255) DEFAULT NULL,
  `manual` bit(1) NOT NULL DEFAULT b'0',
  `duration` int(11) DEFAULT NULL,
  KEY `FKC80F71B4738B879` (`ActivityID`),
  CONSTRAINT `FKC80F71B4738B879` FOREIGN KEY (`ActivityID`) REFERENCES `Facet_MovesPlace` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
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
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `guestId_index` (`guestId`)
) ENGINE=InnoDB AUTO_INCREMENT=8768 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=234 DEFAULT CHARSET=utf8;
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
  `messageDisplayCountersStorage` mediumtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2856 DEFAULT CHARSET=utf8;
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
  CONSTRAINT `FK_BUDDY` FOREIGN KEY (`buddy_id`) REFERENCES `CoachingBuddies` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=606 DEFAULT CHARSET=utf8;
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
  KEY `name` (`name`),
  KEY `guestId_index` (`guestId`)
) ENGINE=InnoDB AUTO_INCREMENT=140065 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UpdateWorkerTask`
--

DROP TABLE IF EXISTS `UpdateWorkerTask`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UpdateWorkerTask` (
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
  `startTime` bigint(20) DEFAULT NULL,
  `endTime` bigint(20) DEFAULT NULL,
  `workerThreadName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `apiKeyId` (`apiKeyId`),
  KEY `status_index` (`status`),
  KEY `serverUUID_index` (`serverUUID`),
  KEY `apiKeyId_index` (`apiKeyId`),
  KEY `connectorName_index` (`connectorName`),
  KEY `timeScheduled_index` (`timeScheduled`),
  KEY `updateType_index` (`updateType`),
  KEY `guestId_index` (`guestId`),
  KEY `objectTypes_index` (`objectTypes`),
  KEY `retries_index` (`retries`)
) ENGINE=InnoDB AUTO_INCREMENT=2190764 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=404565 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=217 DEFAULT CHARSET=utf8;
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

-- Dump completed on 2014-11-10  6:01:09
