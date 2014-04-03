ALTER TABLE Facet_EvernoteNotebook DROP COLUMN publishing;
ALTER TABLE Facet_EvernoteNotebook ADD COLUMN publishingPublicDescription longText NULL;
ALTER TABLE Facet_EvernoteNotebook ADD COLUMN publishingUri varchar(255) NULL;
ALTER TABLE Facet_EvernoteNotebook ADD COLUMN publishingNoteOrderValue int(11) NULL;
