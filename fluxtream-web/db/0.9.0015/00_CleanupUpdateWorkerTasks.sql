-- This removes all the entries from UpdateWorkerTask other than the completed 
-- history updates, which are currently used to determine if a connector has
-- completed its initial update or not.  

-- There had been a problem prior to commit
-- 738beef2040829b509577750e20dd13c29c0dd1a on 5/24/13 where scheduled
-- tasks were incorrectly being left with assigned serverUUIDs and
-- then treated inconsistently within the system.  In particular, the
-- pollScheduledUpdateWorkerTasks routine which decides what tasks to
-- schedule was ignoring them so they did not execute.  However, the
-- named queries in updateWorkerTasks that were used by
-- updateConnector to decide whether or not to schedule an update for
-- a given connector were treating these updates as valid, thus
-- preventing further updates from being scheduled.  The net result
-- was connector updates not happening in cases when they should.
-- This meant that items in UpdateWorkerTask table with status=0 and a
-- non-empty serverUUID would just accumulate.

-- The new behavior only fills in serverUUID for updates being
-- actively processed (status=1) and treats all scheduled tasks
-- (status=0) as valid even if from a previous server run.  However,
-- applying that behavior to old tables clogged with cruft would lead to 
-- spurious extra overlapping updates which could cause problems.

-- This sql migration gets rid of all the cruft from UpdateWorkerTask
-- that could potentially cause problems.
DELETE FROM UpdateWorkerTask WHERE not (status=2 AND updateType=2);
