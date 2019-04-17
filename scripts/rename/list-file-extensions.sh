#!/bin/bash

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

if [[ -z "$1" ]]
then
	echo "Error: no config directory specified" >&2
	cat << EOF
usage: $0 <config-director>

	Lists all the file extensions of files that will be 
	processed by rename.sh when using config in config-dir
EOF
	exit 1	
fi

confdir="$( cd "$1" >/dev/null 2>&1 && pwd )"

if [[ ! -f "$confdir/oldforms.txt" ]]
then
	echo "Error: $confdir does not seem to be a valid conf directory for $0" >&2
	exit 1	
fi

if [[ ! -e "${confdir}/renameignore" ]]
then
	touch "${confdir}/renameignore"
fi

if [[ ! -e "${confdir}/renameselect" ]]
then
	echo '*' > "${confdir}/renameselect"
fi

#find out which file extensions rename.sh will process

echo -e "These are the file extensions \e[32mrename.sh\e[0m will be processing, starting at the current directory"
echo "If you don't like that, change the file ${confdir}/renameignore "
find . -type f | grep -E -f "${confdir}/renameselect" | grep -v -E -f "${confdir}/renameignore" | sed -r -e 's/^.+\.([^\.])/\1/g' | sort | uniq >&2
exit 1