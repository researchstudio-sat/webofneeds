#!/usr/bin/bash
usage(){
cat << EOF
usage: $0 <widoco-jar> [only-ont]

	Generates (or re-generates) all the ontology documentation.
	Parameters: 
		widoco-jar: the path to the widoco jar-with-dependencies to be run
		only-ont: generate docs only for the specified ontology
	Hint: Widoco can be downloaded from https://github.com/dgarijo/Widoco
EOF
}
script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

if [[ -z ${1+x} || ! -r $1 ]]
then
	usage
	exit 1
fi
widoco_jar=$1
if [[ ! -z ${2+x} ]]
then
	only_ont=$2
fi

output_base=${script_path}/../../../../../pub
ontology_root=${script_path}/../../ontology
echo "(re)generating ontology documentation in: ${output_base}"
onts=(core message agreement modification)
rewrite_base="https://researchstudio-sat.github.io/webofneeds/ontologies"
ext_onts=(buddy chat group hold review schema)


function generate_for_ontology() {
	ont=$1
	is_ext=$2
	if [[ ! -z ${only_ont} && ${only_ont} != ${ont} ]]
	then
		# the user specified only_ont and it's not the current one. skip.
		echo "Only generating docs for ontology ${only_ont}. Skipping ${ont} (main)"
		return
	fi
	if [[ ${is_ext} = true ]]
	then
		output_path="ext/${ont}"
		ont_src_dir="${ontology_root}/ext"
		ont_file_stem="won-ext-${ont}"
	else 
		output_path="${ont}"
		ont_src_dir="${ontology_root}"
		ont_file_stem="won-${ont}"
	fi
	ont_widoco_dir="${ont_src_dir}/${ont_file_stem}-widoco"
	ont_file_name="${ont_file_stem}.ttl"
	ont_file="${ont_src_dir}/${ont_file_name}"
	config_file="${ont_widoco_dir}/widoco.conf"
	if [[ -r ${config_file} ]]
	then
		config_file_opt="-confFile ${config_file}"
	fi
	mkdir -p "${output_path}"
	echo "generating documentation for ${ont} in ${output_path}" 
	java -jar ${widoco_jar} -ontFile "${ont_file}" -outFolder "${output_path}" \
		-oops -rewriteAll -htaccess -rewriteBase '' -webVowl \
		-licensius ${config_file_opt}
	java -jar ${widoco_jar} -ontFile "${ont_file}" -outFolder "${output_path}" -crossRef -rewriteAll
	cp "${output_path}/index-en.html" "${output_path}/index.html"
	for section_html_file in ${output_path}/sections/*.html
	do
		section_html_filename=${section_html_file##*/}
		section_md_filename=${section_html_filename%.html}.md
		section_md_file=${ont_widoco_dir}/${section_md_filename}
		if [[ -r ${section_md_file} ]]
		then
			echo -e "replacing template text in ${section_html_filename} with content of ${section_md_file}"
			perl -i -0pe "s@(?<=<span class=\"markdown\">)[^>]+(?=</span>)@\`cat ${section_md_file}\`@e" ${section_html_file}
		fi
	done
	if [[ -d ${ont_widoco_dir}/img ]]
	then
		echo "copying images folder"
		cp -a ${ont_widoco_dir}/img ${output_path}
	fi
}

mkdir -p ${output_base}
mkdir -p ${output_base}/ext
cd "${output_base}"

for ont in ${onts[@]} 
do
	generate_for_ontology ${ont} false
done

for ont in ${ext_onts[@]} 
do
	generate_for_ontology ${ont} true
done