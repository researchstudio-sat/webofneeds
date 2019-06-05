#!/usr/bin/bash
#
usage(){
cat << EOF
usage: $0 [FORCE]

	Finds all .trig files recursively from the working directory and 
	prepends prefix declarations for con: and match: 

	Parameters:
		FORCE - really change files
				without this parameter, files are not changed but written to /tmp/tmpfile.trig
EOF
}

if [[ $1 == "-h" || $1 == "--help" ]]
then
	usage
	exit 0
fi

if [[ $1 == "FORCE" ]]
then
	FORCE=true
else 
	FORCE=false
fi

script_name=${BASH_SOURCE[0]##*/}
function error_handler() {
  echo "Error occurred in ${script_name} at line: ${1}."
  echo "Line exited with status: ${2}"
}

trap 'error_handler ${LINENO} $?' ERR

set -o errexit
set -o errtrace
set -o nounset

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
shopt -s globstar
if (${FORCE})
then
	echo -e "\e[31myou said FORCE, will actually change files\e[0m"
else 
	echo -e "\e[32mDry run. Not changing files. Use the FORCE parameter to make changes\e[0m"
fi
echo "Recursively searching all trig files, this might take a while..."
for file in `find . -type f | grep -E ".trig$" | grep -v -E -f "${script_path}/renameignore"`
do
	if [[ ! -f ${file} ]]
	then
		continue
	fi
	echo -ne "processing $file: "
	prepend_file=/tmp/prepend.trig
	rm -f ${prepend_file}
	touch ${prepend_file}
	grep -q 'con:' $file && ! grep -q 'won/content#' $file && \
		echo "@prefix con: <https://w3id.org/won/content#> ." >> ${prepend_file} 
	grep -q 'match:' $file && ! grep -q 'won/matching#' $file && \
		echo "@prefix match: <https://w3id.org/won/matching#> ." >> ${prepend_file}
	if [[ ! -s ${prepend_file} ]]
	then
		## prepend file is empty, nothing to do
		echo  -e "\e[96mprefix not used or already defined, not touching file.\e[0m"
		continue
	fi
	echo -en "\e[33madding prefixes...\e[0m"
	cat ${prepend_file} > /tmp/tmpfile.trig
	cat ${file} >> /tmp/tmpfile.trig
	if (${FORCE})
	then
		mv /tmp/tmpfile.trig $file
		echo -e "\e[32m done.\e[0m"
	else 
		echo -e "\e[32m dry run, leaving file untouched.\e[0m Output is in /tmp/tmpfile.trig"
	fi
done 
