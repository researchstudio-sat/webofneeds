BEGIN;

ALTER TABLE connection ADD COLUMN last_update TIMESTAMP DEFAULT now() NOT NULL;
ALTER TABLE connection ADD COLUMN version INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE connection ADD COLUMN datasetholder_id bigint;
ALTER TABLE connection ADD COLUMN parent_need_id bigint;

ALTER TABLE message_event ADD COLUMN version integer DEFAULT 0 NOT NULL;
ALTER TABLE message_event ADD COLUMN datasetholder_id BIGINT;
ALTER TABLE message_event ADD COLUMN eventcontainer_id BIGINT;

ALTER TABLE need ADD COLUMN version integer DEFAULT 0 NOT NULL;
ALTER TABLE need ADD COLUMN last_update TIMESTAMP DEFAULT now() NOT NULL;
ALTER TABLE need ADD COLUMN datatsetholder_id BIGINT;

ALTER TABLE rdf_datasets ADD COLUMN version integer DEFAULT 0 NOT NULL;
ALTER TABLE rdf_datasets DROP CONSTRAINT rdf_datasets_pkey;
ALTER TABLE rdf_datasets ALTER COLUMN dataseturi TYPE VARCHAR(255);
ALTER TABLE rdf_datasets ADD COLUMN id BIGINT PRIMARY KEY NOT NULL;

CREATE TABLE connection_container
(
    last_update TIMESTAMP DEFAULT now() NOT NULL,
    version INTEGER DEFAULT 0 NOT NULL,
    need_id BIGINT PRIMARY KEY NOT NULL
);
CREATE TABLE connection_container_connection
(
    connection_container_need_id BIGINT NOT NULL,
    connections_id BIGINT NOT NULL
);
CREATE TABLE event_container
(
    parent_type VARCHAR(31) NOT NULL,
    id BIGINT PRIMARY KEY NOT NULL,
    last_update TIMESTAMP DEFAULT now() NOT NULL,
    parent_uri VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 0 NOT NULL,
    connection_id BIGINT,
    need_id BIGINT
);
CREATE TABLE event_container_message_event
(
    event_container_id BIGINT NOT NULL,
    events_id BIGINT NOT NULL
);
CREATE TABLE need_rdf_datasets
(
    need_id BIGINT NOT NULL,
    attachmentdatasetholders_id BIGINT NOT NULL
);

ALTER TABLE connection ADD FOREIGN KEY (datasetholder_id) REFERENCES rdf_datasets (id);
ALTER TABLE connection ADD FOREIGN KEY (parent_need_id) REFERENCES connection_container (need_id);
ALTER TABLE connection_container ADD FOREIGN KEY (need_id) REFERENCES need (id);
ALTER TABLE connection_container_connection ADD FOREIGN KEY (connection_container_need_id) REFERENCES connection_container (need_id);
ALTER TABLE connection_container_connection ADD FOREIGN KEY (connections_id) REFERENCES connection (id);
CREATE UNIQUE INDEX uk_rog2qpluushvb0b5wsgfeoclw ON connection_container_connection (connections_id);
ALTER TABLE event_container ADD FOREIGN KEY (connection_id) REFERENCES connection (id);
ALTER TABLE event_container ADD FOREIGN KEY (need_id) REFERENCES need (id);
CREATE UNIQUE INDEX uk_himeul255pjfhbm4gr9cdxygn ON event_container (parent_uri);
ALTER TABLE event_container_message_event ADD FOREIGN KEY (event_container_id) REFERENCES event_container (id);
ALTER TABLE event_container_message_event ADD FOREIGN KEY (events_id) REFERENCES message_event (id);
CREATE UNIQUE INDEX uk_1odm07pn1n41xshujimo00myi ON event_container_message_event (events_id);
ALTER TABLE message_event ADD FOREIGN KEY (datasetholder_id) REFERENCES rdf_datasets (id);
ALTER TABLE message_event ADD FOREIGN KEY (eventcontainer_id) REFERENCES event_container (id);
ALTER TABLE need ADD FOREIGN KEY (datatsetholder_id) REFERENCES rdf_datasets (id);
ALTER TABLE need_rdf_datasets ADD FOREIGN KEY (need_id) REFERENCES need (id);
ALTER TABLE need_rdf_datasets ADD FOREIGN KEY (attachmentdatasetholders_id) REFERENCES rdf_datasets (id);
CREATE UNIQUE INDEX uk_517xcon8opn02vk3xytx4vdih ON need_rdf_datasets (attachmentdatasetholders_id);
CREATE UNIQUE INDEX uk_ltf0bds880qjgq8iqoia62he2 ON rdf_datasets (dataseturi);

COMMIT;

