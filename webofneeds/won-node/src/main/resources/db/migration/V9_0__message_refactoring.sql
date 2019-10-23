ALTER TABLE atom ALTER COLUMN state SET NOT NULL;

-- DatasetHolder is no longer unique for MessageEvents 
DROP UNIQUE INDEX IDX_ME_UNIQUE_DATASETHOLDER_ID;
/* TODO
 *  - change constraint from MessageEvent @UniqueConstraint(name = "IDX_ME_UNIQUE_MESSAGE_URI", columnNames = { "messageURI", "parentURI" }),
 *  - manyToOne rel message->datasetholder
 */