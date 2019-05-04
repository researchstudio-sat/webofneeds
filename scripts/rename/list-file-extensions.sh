#!/bin/bash

usage (){	
cat << EOF
usage: $0 <config-director>

	Lists all the file extensions of files that will be 
	processed by rename.sh when using config in config-dir
EOF
}
script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source ${script_path}/common.sh $* 

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