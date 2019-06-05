# common functionality for all rename scripts
# expects a usage() function already defined at the point this file is sourced.

script_name=$0

function error_handler() {
  echo "Error occurred in ${script_name} at line: ${1}."
  echo "Line exited with status: ${2}"
}

trap 'error_handler ${LINENO} $?' ERR

set -o errexit
set -o errtrace
set -o nounset

if [[ -z ${1+x} ]]
then
	echo "Error: no config directory specified" >&2
	usage
	exit 1	
fi

if [[ ! -d "$1" ]]
then
	echo "Error: $1 is not a directory" >&2
	exit 2
fi

confdir="$( cd "$1" >/dev/null 2>&1 && pwd )"

if [[ ! -d "$confdir" ]]
then
	echo "Error: $confdir is not a directory" >&2
	exit 2	
fi

if [[ ! -f "$confdir/oldforms.txt" ]]
then
	echo "Error: $confdir does not seem to be a valid conf directory for $0" >&2
	exit 3	
fi

oldforms_file=${confdir}/oldforms.txt
newforms_file=${confdir}/newforms.txt
oldforms_file_expanded=${confdir}/generated/oldforms-expanded.txt
newforms_file_expanded=${confdir}/generated/newforms-expanded.txt
sed_script_file=${confdir}/generated/sed-changefile-regexes.txt
sed_script_file_db_migration=${confdir}/generated/sed-db-migration-regexes.txt
grep_script_file=${confdir}/generated/grep-checkfile-regex.txt