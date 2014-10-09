alter table Facet_SmsEntry modify emailId varchar(100);
alter table Facet_SmsEntry ADD INDEX `emailId` (`emailId`);
alter table Facet_CallLog modify emailId varchar(100);
alter table Facet_CallLog ADD INDEX `emailId` (`emailId`);