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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `Application` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` longtext,
  `guestId` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `sharedSecret` varchar(255) DEFAULT NULL,
  `uid` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `uid` (`uid`),
  KEY `guestId` (`guestId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AuthorizationCodeResponse` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `authorizationCodeId` bigint(20) NOT NULL,
  `granted` char(1) NOT NULL,
  `guestId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
