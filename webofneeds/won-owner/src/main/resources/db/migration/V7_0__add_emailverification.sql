BEGIN;
CREATE TABLE verificationtoken
(
    id bigint NOT NULL,
    expirydate TIMESTAMP,
    token VARCHAR(255),
    user_id bigint NOT NULL,
    CONSTRAINT verificationtoken_pkey PRIMARY KEY (id),
    CONSTRAINT verificationtoken_userid FOREIGN KEY (user_id)
        REFERENCES wonuser (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

ALTER TABLE wonuser ADD COLUMN registrationdate TIMESTAMP;
ALTER TABLE wonuser ADD COLUMN email_verified BOOLEAN Default false;

COMMIT;