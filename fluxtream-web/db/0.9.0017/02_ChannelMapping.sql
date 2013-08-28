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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
