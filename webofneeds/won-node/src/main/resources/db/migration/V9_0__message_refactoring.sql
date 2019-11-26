ALTER TABLE atom ALTER COLUMN state SET NOT NULL;

-- DatasetHolder is no longer unique for MessageEvents 
DROP INDEX IF EXISTS IDX_ME_UNIQUE_DATASETHOLDER_ID;

-- message_event is now only unique with respect to messageuri and parenturi
DROP INDEX IF EXISTS IDX_ME_UNIQUE_MESSAGE_URI;
CREATE UNIQUE INDEX IDX_ME_UNIQUE_MESSAGE_URI_PER_PARENT ON message_event (messageuri, parenturi);

-- drop correspondingremotemessageuri
ALTER TABLE message_event DROP column correspondingremotemessageuri;
DROP INDEX IF EXISTS IDX_ME_UNIQUE_CORREXPONDING_REMOTE_MESSAGE_URI;
DROP INDEX IF EXISTS IDX_ME_INNERMOST_MESSAGE_URI_RECIPIENT_ATOM_URI;
CREATE INDEX IDX_ME_INNERMOST_MESSAGE_URI_RECIPIENT_ATOM_URI on message_event (messageuri, recipientatomuri, innermostmessageuri);

-- drop innermostmessageuri
ALTER TABLE message_event DROP column innermostmessageuri;
DROP INDEX IF EXISTS IDX_ME_INNERMOST_MESSAGE_URI_RECIPIENT_ATOM_URI;
CREATE INDEX IDX_ME_RECIPIENT_ATOM_URI on message_event(messageuri, recipientatomuri);

-- add special fields for response information (respondingToURI, responseContainerURI), so we can make sure we 
-- store only one response from a container
ALTER TABLE message_event ADD COLUMN respondingtouri VARCHAR(255) default null;
ALTER TABLE message_event ADD COLUMN responsecontaineruri VARCHAR(255) default null;
CREATE UNIQUE INDEX IDX_ME_UNIQUE_RESPONSE_PER_CONTAINER on message_event(parenturi, respondingtouri, responsecontaineruri);


-- make essential fields in connection non-nullable
ALTER TABLE connection ALTER COLUMN state SET NOT NULL;
ALTER TABLE connection ALTER COLUMN atomuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN targetatomuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN atomuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN socketuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN remotesocketuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN connectionuri SET NOT NULL;

-- Table: public.messagecontainer_unconfirmed

CREATE TABLE public.pendingconfirmation
(
    id bigint NOT NULL,
    confirmedmessageuri oid,
    confirmingmessageuri character varying(255) COLLATE pg_catalog."default" NOT NULL,
    messagecontainer_id bigint NOT NULL,
    CONSTRAINT pendingconfirmation_pkey PRIMARY KEY (id),
    CONSTRAINT idx_pendingconfirmation_to_container_id UNIQUE (messagecontainer_id, confirmingmessageuri)
,
    CONSTRAINT fk_pendingconfirmation FOREIGN KEY (messagecontainer_id)
        REFERENCES public.message_container (id) MATCH SIMPLE
        ON DELETE CASCADE
)

