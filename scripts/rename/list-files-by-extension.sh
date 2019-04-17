#!/bin/bash

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

if [[ -z "$1" ]]
then
	echo "Error: no config directory specified" >&2
	cat << EOF
usage: $0 <config-directory> <file-extension> [-p]

	Lists all the files ending with ".[file-extension]" that will be 
	processed by rename.sh when using config in config-dir

	options:
		-p		if a file would be affected, print the grep output to stdout
EOF
	exit 1	
fi

confdir="$( cd "$1" >/dev/null 2>&1 && pwd )"

if [[ ! -f "$confdir/oldforms.txt" ]]
then
	echo "Error: $confdir does not seem to be a valid conf directory for $0" >&2
	exit 1	
fi


sed_script_file=${confdir}/generated/sed-changefile-regexes.txt
grep_script_file=${confdir}/generated/grep-checkfile-regex.txt

#find out which files extensions rename.sh will process, by extension
ext=$2

print_grep_output=false
if  [[ $3 = "-p" ]]
then 
	print_grep_output=true
fi

if [[ -z ${ext} ]]
then
	cat << EOF

usage: $0 config-dir file-extension [-p]

	Lists all the files ending with ".[file-extension]" that will be 
	processed by rename.sh when using config in config-dir

	options:
		-p		if a file would be affected, print the grep output to stdout

EOF
	exit 1
fi

checkfile () {
	while read fileToCheck
	do 
		echo -en "checking if ${fileToCheck} would be affected... " >&2
		if (${print_grep_output})
		then
			grep -E -f "${grep_script_file}" "${fileToCheck}" >/dev/null 2>&1 && echo "matching text in ${fileToCheck}:" >&2
			grep --color -E -f "${grep_script_file}" "${fileToCheck}" >&2
		else
			grep -E -f "${grep_script_file}" "${fileToCheck}" >/dev/null 2>&1
		fi
		
		if [[ $? -eq 0 ]]
		then
			echo -e "\e[32myes\e[0m" >&2
			echo "${fileToCheck}" 
		else
			echo -e "no" >&2
		fi
	done
}

# make sure we have up-to-date regex files
${script_path}/generate-regex-files.sh $confdir || exit 1


echo -e "These are the files with extension .${ext} that \e[32mrename.sh\e[0m will be processing, starting at the current directory" >&2
echo "If you don't like that, change the file ${confdir}/renameignore " >&2
find . -type f -name "*.${ext}" | grep -v -E -f "${confdir}/renameignore" | checkfile 
exit 1