#!/bin/bash

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

if [[ -z "$1" ]]
then
	echo "Error: no config directory specified" >&2
	cat << EOF
usage: $0 <config-directory>

	Processes the config for rename.sh found in [config-directory], generating the database migration files in
	[config-directory]/generated

EOF
	exit 1	
fi

confdir="$( cd "$1" >/dev/null 2>&1 && pwd )"

if [[ ! -f "$confdir/oldforms.txt" ]]
then
	echo "Error: $confdir does not seem to be a valid conf directory for $0" >&2
	exit 1	
fi

# make sure we have up-to-date regex files
${script_path}/generate-regex-files.sh $confdir || exit 1

sed_script_file_db_migration=${confdir}/generated/sed-db-migration-regexes.txt

pgbin='/c/Program Files/PostgreSQL/10/bin'

queryfile=${script_path}/queries-for-db-migration.sql

pghost_node=satvm02.researchstudio.at
pgport_node=5432
tmpfile_node=$(mktemp /tmp/psqlmigrateXXXXXX.tmp)
outfile_node=${confdir}/generated/rename-migration-node.sql

pghost_owner=satvm02.researchstudio.at
pgport_owner=5433
tmpfile_owner=$(mktemp /tmp/psqlmigrateXXXXXX.tmp)
outfile_owner=${confdir}/generated/rename-migration-owner.sql



echo -e "\e[32mwriting sql file for migrating node db\e[0m" >&2
echo "obtaining database schema information from ${pghost_node}:${pgport_node}" >&2
winpty "${pgbin}/psql" --host="${pghost_node}" --port="${pgport_node}" --username=won --file="${queryfile}" --output="${tmpfile_node}" -t won_node 
cat ${tmpfile_node} | sed --file="${sed_script_file_db_migration}" | grep '#CHANGED#' | sed -e 's/#CHANGED#//g' > ${outfile_node}
migration_lines=$(wc -l ${outfile_node} | awk '{print $1;}')
echo "  result: script will migrate ${migration_lines} table/field/index names"
rm ${tmpfile_node}

echo -e "\e[32mwriting sql file for migrating owner db\e[0m" >&2
echo "obtaining database schema information from ${pghost_owner}:${pgport_owner}" >&2
winpty "${pgbin}/psql" --host="${pghost_owner}" --port="${pgport_owner}" --username=won --file="${queryfile}" --output="${tmpfile_owner}" -t won_owner 
cat ${tmpfile_owner} | sed --file="${sed_script_file_db_migration}" | grep '#CHANGED#' | sed -e 's/#CHANGED#//g' > ${outfile_owner}
migration_lines=$(wc -l ${outfile_owner} | awk '{print $1;}')
echo "  result: script will migrate ${migration_lines} table/field/index names"
rm ${tmpfile_owner}