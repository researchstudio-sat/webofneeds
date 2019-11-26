-- DatasetHolder is no longer unique for MessageEvents
DROP INDEX IF EXISTS IDX_ME_UNIQUE_DATASETHOLDER_ID;