ALTER TABLE MovesMoveActivity ADD COLUMN activityGroup VARCHAR(255) NULL;
ALTER TABLE MovesPlaceActivity ADD COLUMN activityGroup VARCHAR(255) NULL;
ALTER TABLE MovesMoveActivity ADD COLUMN manual bit(1) NULL;
ALTER TABLE MovesPlaceActivity ADD COLUMN manual bit(1) NULL;
