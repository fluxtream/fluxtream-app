ALTER TABLE `Facet_LastFmRecentTrack` ADD COLUMN `mbid` varchar(255) NULL;
ALTER TABLE `Facet_LastFmRecentTrack` MODIFY `url` mediumtext NULL;