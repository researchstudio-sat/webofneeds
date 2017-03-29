ALTER TABLE connection ADD COLUMN event_container_id BIGINT NOT NULL;
UPDATE connection set event_container_id = (SELECT id FROM event_container WHERE connection_id = connection.id);
ALTER TABLE event_container DROP COLUMN connection_id;

ALTER TABLE need ADD COLUMN event_container_id BIGINT NOT NULL;
UPDATE need set event_container_id = (SELECT id FROM event_container WHERE need_id = need.id);
ALTER TABLE event_container DROP COLUMN need_id;
