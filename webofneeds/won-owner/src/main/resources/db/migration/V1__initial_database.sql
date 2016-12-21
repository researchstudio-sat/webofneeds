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
CREATE INDEX idx_facet_neeeduri_typeuri ON facet (needuri, typeuri);
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
CREATE UNIQUE INDEX uk_klmt3b32i4hglxdo9cpqw2oqs ON need (needuri);
CREATE TABLE ownerapplication
(
  id BIGINT PRIMARY KEY NOT NULL,
  incomingendpoint VARCHAR(255),
  ownerapplicationid VARCHAR(255)
);

CREATE TABLE need_ownerapp
(
  need_id BIGINT NOT NULL,
  owner_application_id BIGINT NOT NULL,
  CONSTRAINT fk_3xlguycmlc8m1ucka16x8wa60 FOREIGN KEY (need_id) REFERENCES need (id),
  CONSTRAINT fk_avc8715hqo6exf75d4odkihao FOREIGN KEY (owner_application_id) REFERENCES ownerapplication (id)
);
CREATE TABLE needdraft
(
  id BIGINT PRIMARY KEY NOT NULL,
  content VARCHAR(10000),
  drafturi VARCHAR(255)
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

CREATE TABLE queuenames
(
  owner_application_id BIGINT NOT NULL,
  queuename VARCHAR(255),
  CONSTRAINT fk_nkcneddwmvrpm8tnegf2bbfxa FOREIGN KEY (owner_application_id) REFERENCES ownerapplication (id)
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
CREATE TABLE wonuser
(
  id BIGINT PRIMARY KEY NOT NULL,
  email VARCHAR(255),
  password VARCHAR(255),
  role VARCHAR(255),
  username VARCHAR(255)
);
CREATE TABLE user_drafturis
(
  user_id BIGINT NOT NULL,
  drafturis BYTEA,
  CONSTRAINT fk_3ixjhy6vqaydv71bjy934tfvo FOREIGN KEY (user_id) REFERENCES wonuser (id)
);
CREATE TABLE userneed
(
  id BIGINT PRIMARY KEY NOT NULL,
  conversations BOOLEAN,
  creationdate TIMESTAMP NOT NULL,
  matches BOOLEAN,
  requests BOOLEAN,
  uri VARCHAR(255)
);


CREATE TABLE wonuser_userneed
(
  wonuser_id BIGINT NOT NULL,
  userneeds_id BIGINT NOT NULL,
  CONSTRAINT fk_40y1dl6r6hri4jvnvagtigcrt FOREIGN KEY (wonuser_id) REFERENCES wonuser (id),
  CONSTRAINT fk_e6i3wwqxp9hxtrnl7yvnehqoj FOREIGN KEY (userneeds_id) REFERENCES userneed (id)
);
CREATE UNIQUE INDEX uk_e6i3wwqxp9hxtrnl7yvnehqoj ON wonuser_userneed (userneeds_id);
CREATE UNIQUE INDEX uk_k6kk76mj9cu06decrq50j0yo2 ON ownerapplication (ownerapplicationid);
CREATE UNIQUE INDEX uk_3yv0myjeqegxqc05wla0gjjsa ON wonuser (username);
CREATE UNIQUE INDEX uk_r3j1kq5bbo8ty6ron07x6ug61 ON userneed (uri);
CREATE UNIQUE INDEX uk_89863rf9scgijkgdr6bqvudya ON wonnode (wonnodeuri);
CREATE UNIQUE INDEX uk_cl3nn37r2f2w01qh39fnrx30g ON needdraft (id, drafturi);
CREATE UNIQUE INDEX uk_t60803mse9t5kxol2irala16p ON needdraft (drafturi);
CREATE UNIQUE INDEX uk_qfurj5uthoxn2btm8d4m465h5 ON bastate (coordinatoruri, participanturi);
CREATE UNIQUE INDEX uk_kyf146n11n7mkqxptjkhbhx4u ON connection (connectionuri);
CREATE UNIQUE INDEX idx_unique_connection ON connection (needuri, remoteneeduri, typeuri);
CREATE INDEX idx_connection_needuri_remoteneeduri ON connection (needuri, remoteneeduri);
CREATE UNIQUE INDEX uk_oyrkoqlwbhvfuykswdch6mcsq ON match (fromneed, toneed, originator);

COMMIT;