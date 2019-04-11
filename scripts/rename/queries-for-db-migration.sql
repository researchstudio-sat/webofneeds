-- these queries generate DDL ALTER statements for bulk renaming.
SELECT 'ALTER TABLE ' || table_name || ' RENAME COLUMN ' || column_name || ' TO ' || '' || column_name || ' ;'  FROM information_schema.columns WHERE table_schema = 'public';
SELECT 'ALTER TABLE ' || table_name || ' RENAME TO ' || '' || table_name || ' ;'  FROM information_schema.tables WHERE table_schema = 'public';
SELECT 'ALTER INDEX ' || indexname || ' RENAME TO ' || '' || indexname || ' ;'  FROM pg_indexes WHERE schemaname = 'public';


