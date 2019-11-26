-- DatasetHolder is no longer unique for MessageEvents
DROP INDEX IF EXISTS IDX_ME_UNIQUE_DATASETHOLDER_ID;
-- add the field for the serialized set of unconfirmed message uris
ALTER TABLE message_container ADD COLUMN unconfirmed bytea NOT NULL;