ALTER TABLE Facet_FlickrPhoto MODIFY latitude float;
ALTER TABLE Facet_FlickrPhoto MODIFY longitude float;
UPDATE Facet_FlickrPhoto SET latitude = NULL WHERE latitude=0;
UPDATE Facet_FlickrPhoto SET longitude = NULL WHERE longitude=0;