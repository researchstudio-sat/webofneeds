BEGIN;

ALTER TABLE wonuser ADD COLUMN accepted_tos BOOLEAN Default false;

COMMIT;