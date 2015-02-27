CREATE TABLE `GuestDetails` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `city_id` int(11) DEFAULT NULL,
  `countryCode` varchar(255) DEFAULT NULL,
  `guestId` bigint(20) NOT NULL,
  `parseInstallationsStorage` longtext,
  `phoneNumber` varchar(255) DEFAULT NULL,
  `profilePresentation` longtext,
  `website` varchar(255) DEFAULT NULL,
  `accessToken` varchar(255) DEFAULT NULL,
  `refreshToken` varchar(255) DEFAULT NULL,
  `expires` bigint(20) NOT NULL,
  `avatarImageURL` varchar(255) DEFAULT NULL,
  `coachCategory` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `guestId` (`guestId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `Post` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `body` longtext,
  `creationTime` bigint(20) NOT NULL,
  `fromGuestId` bigint(20) NOT NULL,
  `lastUpdateTime` bigint(20) NOT NULL,
  `toGuestId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `lastUpdateTime` (`lastUpdateTime`),
  KEY `creationTime` (`creationTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PostComment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `body` longtext,
  `creationTime` bigint(20) NOT NULL,
  `fromGuestId` bigint(20) NOT NULL,
  `lastUpdateTime` bigint(20) NOT NULL,
  `toGuestId` bigint(20) NOT NULL,
  `post_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKBBF4A17FA1B47E1` (`post_id`),
  KEY `lastUpdateTime` (`lastUpdateTime`),
  KEY `creationTime` (`creationTime`),
  CONSTRAINT `FKBBF4A17FA1B47E1` FOREIGN KEY (`post_id`) REFERENCES `Post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
