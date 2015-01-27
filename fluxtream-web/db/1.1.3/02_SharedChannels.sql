CREATE TABLE `SharedChannels` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `buddy_id` bigint(20) DEFAULT NULL,
  `channelMapping_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKFD3DCB7568CEBFC9` (`buddy_id`),
  KEY `FKFD3DCB752E0740A3` (`channelMapping_id`),
  CONSTRAINT `FKFD3DCB752E0740A3` FOREIGN KEY (`channelMapping_id`) REFERENCES `ChannelMapping` (`id`),
  CONSTRAINT `FKFD3DCB7568CEBFC9` FOREIGN KEY (`buddy_id`) REFERENCES `CoachingBuddies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;