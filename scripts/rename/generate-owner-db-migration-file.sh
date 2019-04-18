#!/bin/bash

script_name=$0

function error_handler() {
  echo "Error occurred in ${script_name} at line: ${1}."
  echo "Line exited with status: ${2}"
}

trap 'error_handler ${LINENO} $?' ERR

set -o errexit
set -o errtrace
set -o nounset

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pgbin='/c/Program Files/PostgreSQL/10/bin'

queryfile=${script_path}/queries-for-db-migration.sql
pghost=satvm05.researchstudio.at
pgport=5433

outfile=${script_path}/generated/migration-template-owner.sql

echo "writing owner db migration template " >&2
winpty "${pgbin}/psql" --host="${pghost}" --port="${pgport}" --username=won --file="${queryfile}" --output="${outfile}" -t -q won_owner 
