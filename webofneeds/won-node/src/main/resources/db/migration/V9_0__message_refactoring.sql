ALTER TABLE atom ALTER COLUMN state SET NOT NULL;

-- DatasetHolder is no longer unique for MessageEvents 
DROP UNIQUE INDEX IDX_ME_UNIQUE_DATASETHOLDER_ID;

/* TODO
 *  [x] change constraint from MessageEvent @UniqueConstraint(name = "IDX_ME_UNIQUE_MESSAGE_URI", columnNames = { "messageURI", "parentURI" }),
 *  [ ] manyToOne rel message->datasetholder
 */
-- message_event is now only unique with respect to messageuri and parenturi
DROP INDEX  IDX_ME_UNIQUE_MESSAGE_URI;
CREATE UNIQUE INDEX IDX_ME_UNIQUE_MESSAGE_URI_PER_PARENT ON message_event (messageuri, parenturi);

-- drop correspondingremotemessageuri
ALTER TABLE message_event DROP column correspondingremotemessageuri;
DROP INDEX IDX_ME_UNIQUE_CORREXPONDING_REMOTE_MESSAGE_URI;
DROP INDEX IDX_ME_INNERMOST_MESSAGE_URI_RECIPIENT_ATOM_URI;
CREATE INDEX IDX_ME_INNERMOST_MESSAGE_URI_RECIPIENT_ATOM_URI on message_event (messageURI, recipientAtomURI, innermostMessageURI);

-- drop innermostmessageuri
ALTER TABLE message_event DROP column innermostmessageuri;
DROP INDEX IDX_ME_INNERMOST_MESSAGE_URI_RECIPIENT_ATOM_URI;
CREATE INDEX IDX_ME_RECIPIENT_ATOM_URI on message_event(messageURI, recipientAtomURI) 

-- make essential fields in connection non-nullable
ALTER TABLE connection ALTER COLUMN state SET NOT NULL;
ALTER TABLE connection ALTER COLUMN atomuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN targetatomuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN atomuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN socketuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN remotesocketuri SET NOT NULL;
ALTER TABLE connection ALTER COLUMN connectionuri SET NOT NULL;

-- Table: public.messagecontainer_unconfirmed

CREATE TABLE messagecontainer_unconfirmed
(
    messagecontainer_id bigint NOT NULL,
    unconfirmed character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT fkgrs8fwpa6quslrln90ntcqs26 FOREIGN KEY (messagecontainer_id)
        REFERENCES public.message_container (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
)
