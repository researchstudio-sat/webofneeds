BEGIN;
-- allows for per-user keystores
CREATE TABLE keystore
(
    keystore_data OID default NULL,
    id BIGINT PRIMARY KEY NOT NULL
);

-- holds the encrypted keystore passwords
CREATE TABLE keystore_password
(
    encrypted_password VARCHAR NOT NULL,
    id BIGINT PRIMARY KEY NOT NULL
);

-- add references to keystore and keystore_password to user
ALTER TABLE wonuser ADD COLUMN keystore_id BIGINT DEFAULT NULL;
ALTER TABLE wonuser ADD COLUMN keystore_password_id BIGINT DEFAULT NULL;
ALTER TABLE wonuser ADD FOREIGN KEY (keystore_id) REFERENCES keystore ON DELETE CASCADE;
ALTER TABLE wonuser ADD FOREIGN KEY (keystore_password_id) REFERENCES keystore_password ON DELETE CASCADE;

-- series must be unique in the persistent logins
CREATE UNIQUE INDEX IDX_UNIQUE_PERSISTENT_LOGINS_SERIES ON persistent_logins(series);
-- link persistent logins with keystore passwords
ALTER TABLE persistent_logins ADD COLUMN keystore_password_id BIGINT DEFAULT NULL;
ALTER TABLE wonuser ADD FOREIGN KEY (keystore_password_id) REFERENCES keystore_password ON DELETE CASCADE;

COMMIT;