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

output_base=${script_path}/../../../../../../doc
cd "${output_base}"
ontology_root=${script_path}/../../
echo "(re)generating ontology documentation in: ${output_base}"
mkdir -p ${output_base}
mkdir -p ${output_base}/ext
onts=(won-core won-message won-agreement won-modification)
rewrite_base="https://raw.githubusercontent.com/researchstudio-sat/webofneeds/master/webofneeds/won-vocab/src/main/resources/ontology/"
ext_onts=(buddy chat group hold review schema)


for ont in ${onts[@]} 
do
	if [[ ! -z ${only_ont} && ${only_ont} != ${ont} ]]
	then
		# the user specified only_ont and it's not the current one. skip.
		echo "Only generating docs for ontology ${only_ont}. Skipping ${ont} (main)"
		continue
	fi
	output_path="${ont}"
	mkdir -p "${output_path}"	
	echo "generating documentation for ${ont} in ${output_path}" 
	java -jar ${widoco_jar} -ontFile "${ontology_root}/${ont}.ttl" -outFolder "${output_path}" \
		-oops -rewriteAll -htaccess -webVowl \
		-licensius -rewriteBase "${rewrite_base}" 
	java -jar ${widoco_jar} -ontFile "${ontology_root}/${ont}.ttl" -outFolder "${output_path}" -crossRef -rewriteAll
done

for ont in ${ext_onts[@]} 
do
	if [[ ! -z ${only_ont} && ${only_ont} != ${ont} ]]
	then
		# the user specified only_ont and it's not the current one. skip.
		echo "Only generating docs for ontology ${only_ont}. Skipping ${ont} (ext)"
		continue
	fi
	output_path="ext/won-ext-${ont}"
	mkdir -p "${output_path}"	
	echo "generating documentation for ${ont} in ${output_path}" 
	java -jar ${widoco_jar} -ontFile "${ontology_root}/ext/won-ext-${ont}.ttl" -outFolder "${output_path}" \
		-oops -rewriteAll -htaccess -webVowl \
		-licensius  -doNotDisplaySerializations -rewriteBase "${rewrite_base}" 
	java -jar ${widoco_jar} -ontFile "${ontology_root}/ext/won-ext-${ont}.ttl" -outFolder "${output_path}" -crossRef -rewriteAll
done