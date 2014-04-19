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
) ENGINE=InnoDB AUTO_INCREMENT=48712 DEFAULT CHARSET=latin1;

