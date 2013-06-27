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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;