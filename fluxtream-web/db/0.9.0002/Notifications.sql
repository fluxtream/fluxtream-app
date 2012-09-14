ALTER TABLE Notifications ADD COLUMN repeated SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE Notifications ADD COLUMN stackTrace longText DEFAULT "";
