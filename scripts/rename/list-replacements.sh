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

# read files into arrays 'oldforms' and 'newforms' (don't try to make this a function, they can't return arrays in bash)
# see https://stackoverflow.com/questions/10582763/how-to-return-an-array-in-bash-without-using-globals
# (same code as in generate-regex-files.sh)
myifs=$IFS
IFS=$'\r\n'; 
read -d '' -r -a oldforms < <(cat ${oldforms_file_expanded}) || true
read -d '' -r -a newforms < <(cat ${newforms_file_expanded}) || true
IFS=${myifs}

if [[ ${#oldforms[@]} != ${#newforms[@]} ]]
then
	echo "ERROR: ${oldforms_file} and ${newforms_file} must have the same number of lines!" >&2
	echo "${oldforms_file}: " $(wc -l ${oldforms_file} | awk '{print $1;}') " lines (generated ${#oldforms[@]} forms)" >&2
	echo "${newforms_file}: " $(wc -l ${newforms_file} | awk '{print $1;}') " lines (generated ${#newforms[@]} forms)" >&2
	exit 1
fi

echo "found ${#oldforms[@]} expressions to replace" >&2
echo "found ${#newforms[@]} replacement expressions" >&2

## replace in URIs, strict enough not to mess up code files
echo "These are the replacements: in the form searchString --> replaceString"
for i in $(seq 0 $(("${#oldforms[@]}"-1)))
do 
	searchString=${oldforms[i]}
	replaceString=${newforms[i]}
	echo "${searchString} --> ${replaceString}" 
done