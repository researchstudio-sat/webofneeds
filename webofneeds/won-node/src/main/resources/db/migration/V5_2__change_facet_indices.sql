DROP INDEX idx_facet_neeeduri_typeuri;
CREATE UNIQUE INDEX idx_unique_facet ON facet (faceturi);

-- the unique condition would have to include the remote facet, but we don't 
-- represent that in the db, so we can't enforce uniqueness.
DROP INDEX IF EXISTS idx_unique_connection CASCADE;  
