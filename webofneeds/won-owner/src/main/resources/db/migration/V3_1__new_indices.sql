BEGIN;
-- new indices on message_event
CREATE INDEX IDX_ME_PARENT_URI_MESSAGE_TYPE on message_event (parentURI, messageType);
CREATE INDEX IDX_ME_PARENT_URI_REFERENCED_BY_OTHER_MESSAGE on message_event(parentURI, referencedByOtherMessage);
CREATE INDEX IDX_ME_PARENT_URI on message_event (parentURI);
CREATE UNIQUE INDEX IDX_ME_UNIQUE_DATASETHOLDER_ID ON message_event (datasetholder_id);
CREATE UNIQUE INDEX IDX_ME_UNIQUE_MESSAGE_URI ON message_event (messageURI);
CREATE UNIQUE INDEX IDX_ME_UNIQUE_CORREXPONDING_REMOTE_MESSAGE_URI on message_event(correspondingRemoteMessageURI);

-- new indices on need
CREATE UNIQUE INDEX IDX_NEED_UNIQUE_EVENT_CONTAINER_ID ON need(event_container_id);
CREATE UNIQUE INDEX IDX_NEED_UNIQUE_DATASETHOLDER_ID ON need (datatsetholder_id);
-- new indices on connection
CREATE UNIQUE INDEX IDX_CONNECTION_UNIQUE_EVENT_CONTAINER_ID on connection(event_container_id);
CREATE UNIQUE INDEX IDX_CONNECTION_UNIQUE_DATASETHOLDER_ID ON connection (datasetholder_id);

-- new indices on need_ownerapp
CREATE UNIQUE INDEX IDX_NO_UNIQUE_NEED_ID_OWNER_APPLICATION_ID ON need_ownerapp(need_id, owner_application_id);
CREATE INDEX IDX_NO_NEED_ID ON need_ownerapp(need_id);
COMMIT;