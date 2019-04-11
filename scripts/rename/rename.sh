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


process_replace (){
	while read replaceInFile
	do
		echo -en "processing ${replaceInFile}... " >&2
		# fast basename: http://www.skybert.net/bash/making-your-bash-programs-run-faster/
		baseName=${replaceInFile##*/}
		newBaseName=$(echo "${baseName}" | sed --file="${sed_script_file}")
		if (! ${FORCE})
		then
			if [[ ${baseName} != ${newBaseName} ]]
			then
				echo -en "\e[33mwould rename\e[0m to ${newBaseName} ... " >&2	
			fi	
		fi
		# fast dirname
		newFileName="${replaceInFile%/*}/${newBaseName}"
		if (${FORCE})
		then
			if [[ ${baseName} != ${newBaseName} ]]
			then

				git ls-files --error-unmatch ${replaceInFile} > /dev/null 2>&1
				managedByGit=$?
				if [[ ${managedByGit} -eq 0 ]]
				then 
					echo -en "\e[33mgit-renaming\e[0m to ${newBaseName} ... " >&2	
					git mv ${replaceInFile} ${newFileName} 			
				else
					echo -en "\e[33mrenaming\e[0m to ${newBaseName} ... " >&2	
					mv ${replaceInFile} ${newFileName} 			
				fi
			fi
			echo -en "\e[33mmodifying\e[0m file ... " >&2	
			sed --file="${sed_script_file}" --follow-symlinks -i ${newFileName}
		fi
		echo "${replaceInFile} -> ${newFileName}" >> ${changedFilesList}
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

write_sed_file

find . -type f | grep -v -E -f "${script_path}/rename-file-filter.txt" | process_replace

