BEGIN;
CREATE TABLE bastate
(
    id BIGINT PRIMARY KEY NOT NULL,
    baphase VARCHAR(255),
    bastateuri VARCHAR(255),
    coordinatoruri VARCHAR(255),
    facettypeuri VARCHAR(255),
    participanturi VARCHAR(255)
);
CREATE TABLE connection
(
    id BIGINT PRIMARY KEY NOT NULL,
    connectionuri VARCHAR(255),
    needuri VARCHAR(255),
    remoteconnectionuri VARCHAR(255),
    remoteneeduri VARCHAR(255),
    state VARCHAR(255),
    typeuri VARCHAR(255)
);
CREATE TABLE facet
(
    id BIGINT PRIMARY KEY NOT NULL,
    needuri VARCHAR(255),
    typeuri VARCHAR(255)
);
CREATE TABLE match
(
    id BIGINT PRIMARY KEY NOT NULL,
    eventid BIGINT,
    fromneed VARCHAR(255),
    originator VARCHAR(255),
    score DOUBLE PRECISION,
    toneed VARCHAR(255)
);
CREATE TABLE message_event
(
    id BIGINT PRIMARY KEY NOT NULL,
    correspondingremotemessageuri VARCHAR(255),
    creationdate TIMESTAMP,
    messagetype VARCHAR(255),
    messageuri VARCHAR(255),
    parenturi VARCHAR(255),
    receiverneeduri VARCHAR(255),
    receivernodeuri VARCHAR(255),
    receiveruri VARCHAR(255),
    referencedbyothermessage BOOLEAN,
    responsemessageuri VARCHAR(255),
    senderneeduri VARCHAR(255),
    sendernodeuri VARCHAR(255),
    senderuri VARCHAR(255)
);
CREATE TABLE need
(
    id BIGINT PRIMARY KEY NOT NULL,
    creationdate TIMESTAMP NOT NULL,
    needuri VARCHAR(255),
    owneruri VARCHAR(255),
    state VARCHAR(255),
    wonnodeuri VARCHAR(255)
);
CREATE TABLE need_ownerapp
(
    need_id BIGINT NOT NULL,
    owner_application_id BIGINT NOT NULL
);
CREATE TABLE ownerapplication
(
    id BIGINT PRIMARY KEY NOT NULL,
    incomingendpoint VARCHAR(255),
    ownerapplicationid VARCHAR(255)
);
CREATE TABLE queuenames
(
    owner_application_id BIGINT NOT NULL,
    queuename VARCHAR(255)
);
CREATE TABLE rdf_datasets
(
    dataseturi BYTEA PRIMARY KEY NOT NULL,
    dataset OID NOT NULL
);
CREATE TABLE rdf_models
(
    modeluri BYTEA PRIMARY KEY NOT NULL,
    model OID NOT NULL
);
CREATE TABLE wonnode
(
    id BIGINT PRIMARY KEY NOT NULL,
    brokercomponent VARCHAR(255),
    brokeruri VARCHAR(255),
    ownerapplicationid VARCHAR(255),
    ownerprotocolendpoint VARCHAR(255),
    startingcomponent VARCHAR(255),
    wonnodeuri VARCHAR(255)
);
CREATE UNIQUE INDEX uk_qfurj5uthoxn2btm8d4m465h5 ON bastate (coordinatoruri, participanturi);
CREATE UNIQUE INDEX uk_kyf146n11n7mkqxptjkhbhx4u ON connection (connectionuri);
CREATE UNIQUE INDEX idx_unique_connection ON connection (needuri, remoteneeduri, typeuri);
CREATE INDEX idx_connection_needuri_remoteneeduri ON connection (needuri, remoteneeduri);
CREATE INDEX idx_facet_neeeduri_typeuri ON facet (needuri, typeuri);
CREATE UNIQUE INDEX uk_oyrkoqlwbhvfuykswdch6mcsq ON match (fromneed, toneed, originator);
CREATE UNIQUE INDEX uk_klmt3b32i4hglxdo9cpqw2oqs ON need (needuri);
ALTER TABLE need_ownerapp ADD FOREIGN KEY (need_id) REFERENCES need (id);
ALTER TABLE need_ownerapp ADD FOREIGN KEY (owner_application_id) REFERENCES ownerapplication (id);
CREATE UNIQUE INDEX uk_k6kk76mj9cu06decrq50j0yo2 ON ownerapplication (ownerapplicationid);
ALTER TABLE queuenames ADD FOREIGN KEY (owner_application_id) REFERENCES ownerapplication (id);
CREATE UNIQUE INDEX uk_89863rf9scgijkgdr6bqvudya ON wonnode (wonnodeuri);

CREATE SEQUENCE hibernate_sequence
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

COMMIT;