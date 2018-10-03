ALTER TABLE connection ADD COLUMN remotefaceturi VARCHAR(255);
CREATE UNIQUE INDEX idx_unique_connection ON connection (needuri, remoteneeduri, faceturi, remotefaceturi);