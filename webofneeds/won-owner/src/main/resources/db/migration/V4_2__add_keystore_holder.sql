BEGIN;
-- allows for per-user keystores
CREATE TABLE keystore
(
    keystore_data oid default NULL,
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT keystore_pkey PRIMARY KEY (id)
);

alter table wonuser add column keystore_id bigint DEFAULT NULL;
COMMIT;