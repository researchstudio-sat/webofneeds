#!/bin/bash

usage(){
cat << EOF
usage: $0 <config-directory> [FORCE]

	Performs recursive renaming of files, directories and file contents according to 
	the configuration found in config-directory

EOF
}
script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

source ${script_path}/common.sh $* 

${script_path}/generate-regex-files.sh "$confdir" || exit 1

# Parameters: 
# $1: the file to process
# Performs the replacements within the file if it is a file and renames it if it is a file or directory
process_replace (){
	while read currentFile
	do
		echo -en "processing ${currentFile}... " >&2
		# fast basename: http://www.skybert.net/bash/making-your-bash-programs-run-faster/
		baseName=${currentFile##*/}
		newBaseName=$(echo "${baseName}" | sed --file="${sed_script_file}")
		if (! ${FORCE})
		then
			if [[ ${baseName} != ${newBaseName} ]]
			then
				echo -en "\e[33mwould rename\e[0m to ${newBaseName} ... " >&2	
			fi	
		fi
		# fast dirname
		newFileName="${currentFile%/*}/${newBaseName}"
		if (${FORCE})
		then
			if [[ -f ${currentFile} ]]
			then
				echo -en "\e[33mmodifying\e[0m file ... " >&2	
				sed --file="${sed_script_file}" --follow-symlinks -i ${currentFile}
			fi
			if [[ ${baseName} != ${newBaseName} ]]
			then
				git ls-files --error-unmatch ${currentFile} > /dev/null 2>&1 && managedByGit=$? || managedByGit=$?
				if [[ ${managedByGit} -eq 0 ]]
				then 
					echo -en "\e[96mgit-mv\e[0m to ${newBaseName} ... " >&2	
					git mv ${currentFile} ${newFileName} 			
				else
					echo -en "\e[96mmv\e[0m to ${newBaseName} ... " >&2	
					mv ${currentFile} ${newFileName} 			
				fi
			fi
		fi
		echo -e "\e[32mdone\e[0m" >&2
	done
}

if [[ -z ${2+x} || $2 != "FORCE" ]]
then
	FORCE=false
	echo -e "\e[32mDry run.\e[0m If you actually want to do this, add the parameter 'FORCE'" >&2
else 
	FORCE=true
	echo "You said 'FORCE'. Starting the replacement process" >&2
fi 

if [[ ! -e "${confdir}/renameignore" ]]
then
	touch "${confdir}/renameignore"
fi

if [[ ! -e "${confdir}/renameselect" ]]
then
	echo '*' > "${confdir}/renameselect"
fi

# list all files, filter using our file filter, and pass to replace
find . -type f | grep -E -f "${confdir}/renameselect" | grep -v -E -f "${confdir}/renameignore" | process_replace

# list all directories, filter by our file filter, compute length of string with awk and add as first attribute,
# sort whole output longest first, remove length attribute, and pass to replace
# have to do it this way so that nested folders get renamed first
find . -type d | grep -E -f "${confdir}/renameselect" | grep -v -E -f "${confdir}/renameignore" | awk '{ print length($0) " " $0; }' | sort -r -n | cut -d ' ' -f 2- | process_replace

echo -e "\e[32mAll done.\e[0m"
