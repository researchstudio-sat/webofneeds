-- add locktable
CREATE TABLE lock
(
    id BIGINT PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL
);
-- add an entry for ownerapplication (in order to avoid duplicate inserts)
INSERT into lock (id, name) values (1,'ownerapplication');