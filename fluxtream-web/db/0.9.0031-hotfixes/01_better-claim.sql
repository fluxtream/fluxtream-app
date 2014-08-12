ALTER TABLE UpdateWorkerTask ADD COLUMN startTime bigint null;
ALTER TABLE UpdateWorkerTask ADD COLUMN endTime bigint null;
ALTER TABLE UpdateWorkerTask ADD COLUMN workerThreadName varchar(255) null;