#!/bin/bash

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

oldforms_file=${script_path}/oldforms.txt
newforms_file=${script_path}/newforms.txt
sed_script_file=${script_path}/generated/sed-changefile-regexes.txt
sed_script_file_db_migration=${script_path}/generated/sed-db-migration-regexes.txt
grep_script_file=${script_path}/generated/grep-checkfile-regex.txt


if [[ $(cat ${oldforms_file} | grep -v '^#' | wc -l ) != $(cat ${oldforms_file} | grep -v '^#' | wc -l) ]]
then
	echo "ERROR: ${oldforms_file} and ${newforms_file} must have the same number of lines!" >&2
	echo "${oldforms_file}: " $(wc -l ${oldforms_file} | awk '{print $1;}') " lines" >&2
	echo "${newforms_file}: " $(wc -l ${newforms_file} | awk '{print $1;}') " lines" >&2
	exit 1
fi

function join_by { local IFS="$1"; shift; echo "$*"; }

# read files into arrays 'oldforms' and 'newforms' (don't try to make this a function, they can't return arrays in bash)
# see https://stackoverflow.com/questions/10582763/how-to-return-an-array-in-bash-without-using-globals
myifs=$IFS
IFS=$'\r\n'; 
read -d '' -r -a oldforms < <(cat ${oldforms_file} | grep -v '^#')
read -d '' -r -a newforms < <(cat ${newforms_file} | grep -v '^#')
IFS=${myifs}

echo "found ${#oldforms[@]} expressions to replace" >&2
echo "found ${#newforms[@]} replacement expressions" >&2

write_sed_file() {
	echo "# sed script for renaming" > ${sed_script_file} 
	## replace in URIs, strict enough not to mess up code files
	for i in $(seq 0 $(("${#oldforms[@]}"-1)))
	do 
		searchString=${oldforms[i]}
		replaceString=${newforms[i]}
		echo "1,$ s@${searchString}@${replaceString}@g" >> ${sed_script_file}
	done
}

write_sed_file_db_migration() {
	echo "# sed script for db migration" > ${sed_script_file_db_migration} 
	## replace in URIs, strict enough not to mess up code files
	for i in $(seq 0 $(("${#oldforms[@]}"-1)))
	do 
		searchString=${oldforms[i]}
		replaceString=${newforms[i]}
		# this regex marks each line in which something is replaced 
		# with the string '#CHANGED#' for further processing
		echo "1,$ s@TO \<\(.*\)${searchString}\(.*\)\> \(#CHANGED#\)*@TO \1${replaceString}\2 #CHANGED#@g" >> ${sed_script_file_db_migration}
	done
}

write_grep_file() {
	echo "# grep expression checking if the file would be changed" > ${grep_script_file} 
	## replace in URIs, strict enough not to mess up code files
	lastIndex=$(("${#oldforms[@]}"-1))
	for i in $(seq 0 ${lastIndex})
	do 
		searchString=${oldforms[i]}
		echo -n "${searchString}" >> ${grep_script_file}
		if [[ i -lt ${lastIndex} ]]
		then
			echo -n "|" >> ${grep_script_file}
		fi
	done
	grep_expr=$(join_by "|" ${oldforms[@]})
	
}

echo "writing ${sed_script_file}" >&2
write_sed_file
echo "writing ${grep_script_file}" >&2
write_grep_file
echo "writing ${sed_script_file_db_migration}" >&2
write_sed_file_db_migration
