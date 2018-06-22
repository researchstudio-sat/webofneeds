BEGIN;

-- add userneed state
ALTER TABLE userneed ADD COLUMN state VARCHAR(255) DEFAULT 'ACTIVE';

COMMIT;