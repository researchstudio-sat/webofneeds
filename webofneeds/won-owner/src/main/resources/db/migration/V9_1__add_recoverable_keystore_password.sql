BEGIN;

ALTER TABLE wonuser ADD COLUMN recoverable_keystore_password_id BIGINT DEFAULT NULL;
ALTER TABLE wonuser ADD FOREIGN KEY (recoverable_keystore_password_id) REFERENCES keystore_password ON DELETE CASCADE;

COMMIT;