CREATE TABLE pushSubscriptions
(
    id BIGINT PRIMARY KEY NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    updated TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_pushsubtouser FOREIGN KEY (user_id) REFERENCES wonuser (id)
);

CREATE UNIQUE INDEX uk_pushsubendpoint ON pushSubscriptions (user_id, endpoint);