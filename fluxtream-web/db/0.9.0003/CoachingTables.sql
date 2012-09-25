CREATE TABLE `CoachingBuddies` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `guestId` bigint(20) NOT NULL,
  `buddyId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `guestId_index` (`guestId`),
  KEY `buddyId_index` (`buddyId`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
CREATE TABLE `SharedConnectors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `connectorName` varchar(255) NOT NULL,
  `filterJson` longtext,
  `buddy_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_BUDDY` (`buddy_id`),
  CONSTRAINT `FK_BUDDY` FOREIGN KEY (`buddy_id`) REFERENCES `CoachingBuddies` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
