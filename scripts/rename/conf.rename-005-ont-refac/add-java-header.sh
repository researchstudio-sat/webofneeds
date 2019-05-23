#!/usr/bin/bash
#
usage(){
cat << EOF
usage: $0 [FORCE]

	Finds all .java files recursively from the working directory and 
	prepends imports for WONMATCH and WONCON.

	Parameters:
		FORCE - really change files
				without this parameter, files are not changed but written to ${tmpfile}
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
echo "Recursively searching all java files, this might take a while..."
tmpfolder=/tmp/addjavaheader
mkdir -p ${tmpfolder}
for file in `find . -type f | grep -E ".java$" | grep -v -E -f "${script_path}/renameignore"`
do
	if [[ ! -f ${file} ]]
	then
		continue
	fi
	echo -ne "processing $file: "
	tmpfile="${tmpfolder}/tmp_${file##*/}"
	prepend_file=/tmp/prepend.java
	rm -f ${prepend_file}
	touch ${prepend_file}
	grep -q 'WONCON\.' $file && ! grep -q 'import won.protocol.vocabulary.WONCON' $file && \
		echo "import won.protocol.vocabulary.WONCON;" >> ${prepend_file} 
	grep -q 'WONMATCH\.' $file && ! grep -q 'import won.protocol.vocabulary.WONMATCH' $file && \
		echo "import won.protocol.vocabulary.WONMATCH;" >> ${prepend_file}
	if [[ ! -s ${prepend_file} ]]
	then
		## prepend file is empty, nothing to do
		echo  -e "\e[96mprefix not used or already defined, not touching file.\e[0m"
		continue
	fi
	echo -en "\e[33madding import statement(s)...\e[0m"
	insertLines=`cat ${prepend_file}`
	cat ${file} | sed -e "/^package.*$/a ${insertLines}" > ${tmpfile}
	if (${FORCE})
	then
		mv ${tmpfile} $file
		echo -e "\e[32m done.\e[0m"
	else 
		echo -e "\e[32m dry run, leaving file untouched.\e[0m Output is in ${tmpfile}"
	fi
	rm -f ${prepend_file}
done 
