#!/bin/bash

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

#find out which file extensions rename.sh will process

echo -e "These are the file extensions \e[32mrename.sh\e[0m will be processing, starting at the current directory"
echo "If you don't like that, change the file ${script_path}/rename-file-filter.txt "
find . -type f | grep -v -E -f "${script_path}/rename-file-filter.txt" | sed -r -e 's/^.+\.([^\.])/\1/g' | sort | uniq >&2
exit 1