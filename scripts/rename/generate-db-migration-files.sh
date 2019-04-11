#!/bin/bash

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
sed_script_file_db_migration=${script_path}/generated/sed-db-migration-regexes.txt

pgbin='/c/Program Files/PostgreSQL/10/bin'

queryfile=${script_path}/queries-for-db-migration.sql

pghost_node=satvm05.researchstudio.at
pgport_node=5433
tmpfile_node=$(mktemp /tmp/psqlmigrateXXXXXX.tmp)
outfile_node=${script_path}/generated/rename-migration-node.sql

pghost_owner=satvm05.researchstudio.at
pgport_owner=5432
tmpfile_owner=$(mktemp /tmp/psqlmigrateXXXXXX.tmp)
outfile_owner=${script_path}/generated/rename-migration-owner.sql

# make sure we have up-to-date regex files
${script_path}/generate-regex-files.sh || exit 1

echo "writing node db migration template " >&2
winpty "${pgbin}/psql" --host="${pghost_node}" --port="${pgport_node}" --username=won --file="${queryfile}" --output="${tmpfile_node}" -t -q won_node 
cat ${tmpfile_node} | sed --file="${sed_script_file_db_migration}" | grep '#CHANGED#' | sed -e 's/#CHANGED#//g' > ${outfile_node}
rm ${tmpfile_node}

echo "writing owner db migration template " >&2
winpty "${pgbin}/psql" --host="${pghost_owner}" --port="${pgport_owner}" --username=won --file="${queryfile}" --output="${tmpfile_owner}" -t -q won_owner 
cat ${tmpfile_owner} | sed --file="${sed_script_file_db_migration}" | grep '#CHANGED#' | sed -e 's/#CHANGED#//g' > ${outfile_owner}
rm ${tmpfile_owner}