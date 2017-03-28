ALTER TABLE event_container DROP COLUMN connection_id;
ALTER TABLE connection ADD COLUMN event_container_id BIGINT NOT NULL;
ALTER TABLE event_container DROP COLUMN need_id;
ALTER TABLE need ADD COLUMN event_container_id BIGINT NOT NULL;