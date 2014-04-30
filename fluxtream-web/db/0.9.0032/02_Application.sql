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
  KEY `guestId` (`guestId`);
) ENGINE=InnoDB DEFAULT CHARSET=utf8;