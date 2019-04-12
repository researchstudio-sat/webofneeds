#!/bin/bash

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

sed_script_file=${script_path}/generated/sed-changefile-regexes.txt
grep_script_file=${script_path}/generated/grep-checkfile-regex.txt

${script_path}/generate-regex-files.sh || exit 1


# writing list of changed files to this file
changedFilesList="changedFiles.tmp"
rm -f ${changedFilesList}
touch ${changedFilesList}
echo "writing changed files to ${changedFilesList}" >&2

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

				git ls-files --error-unmatch ${currentFile} > /dev/null 2>&1
				managedByGit=$?
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
		echo "${currentFile} -> ${newFileName}" >> ${changedFilesList}
		echo -e "\e[32mdone\e[0m" >&2
	done
}

if [[ $1 != "FORCE" ]]
then
	FORCE=false
	echo "Dry run. If you actually want to do this, add the parameter 'FORCE'" >&2
else 
	FORCE=true
	echo "You said 'FORCE'. Starting the replacement process" >&2
fi 

# list all files, filter using our file filter, and pass to replace
find . -type f | grep -v -E -f "${script_path}/rename-file-filter.txt" | process_replace

# list all directories, filter by our file filter, compute length of string with awk and add as first attribute,
# sort whole output longest first, remove length attribute, and pass to replace
# have to do it this way so that nested folders get renamed first
find . -type d | grep -v -E -f "${script_path}/rename-file-filter.txt" | awk '{ print length($0) " " $0; }' | sort -r -n | cut -d ' ' -f 2- | process_replace


