BEGIN;
ALTER TABLE message_event ADD COLUMN innermostmessageuri character varying(255);
CREATE INDEX IDX_ME_INNERMOST_MESSAGE_URI_RECEIVER_NEED_URI ON message_event (messageuri, receiverneeduri, innermostmessageuri, correspondingremotemessageuri);
COMMIT;