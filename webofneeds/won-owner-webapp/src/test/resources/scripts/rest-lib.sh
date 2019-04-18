#/bin/bash

function createRandomString {
	length=$1
	cat /dev/urandom | tr -dc 'a-zA-Z' | fold -w $length | head -n 1
}

rndToken=`createRandomString 5`
HTTP_RESPONSE_OUTPUT_FILE=.lastHttpResponse_${rndToken}


function reportSuccess {
	success=$1
	if [[ success -eq 0 ]]
	then
		echo ": ok"
	else 
		echo ": failed"
	fi
}

function sendUserRequest {
	operation=$1
	user=$2
	server=$3
	path=$4
	json=$5
	method=$6
	returnOutput=$7
	httpStatus=$8
	echo $operation
	curl -i -X ${method} \
		-H 'Content-Type: application/json' \
		-d ''"$json"'' \
		-c ${user}.cookies \
		-b ${user}.cookies \
		${server}$path 2>/dev/null 1>${HTTP_RESPONSE_OUTPUT_FILE}
	return $?
}


function createUser {
	user=$1
	password1=$2
	password2=$3
	server=$4
	expectedStatus=$5
	operation="creating user $user on server $server"
	json="${JSON_CREATE_USER/<NAME>/$user}"
	json="${json/<PASSWORD1>/$password1}"
	json="${json/<PASSWORD2>/$password2}"
	sendUserRequest "$operation" "$user" "$server" "/rest/users/" "$json" POST 
}

function signIn {		
	user=$1
	password=$2
	server=$3
	operation="signing user $user in on server $server"
	json="${JSON_SIGN_IN/<NAME>/$user}"
	json="${json/<PASSWORD>/$password}"
	sendUserRequest "$operation" "$user" "$server" "/rest/users/signin/" "$json" POST
}


function signOut {		
	user=$1
	server=$2
	operation="signing user $user out from server $server"
	json="${JSON_SIGN_OUT/<NAME>/$user}"
	sendUserRequest "$operation" "$user" "$server" "/rest/users/signout/" "$json" POST 
}

function createAtom {
	user=$1
	server=$2
	json=$3
	operation="creating an atom for user $user on server $server"
	sendUserRequest "$operation" "$user" "$server" "/rest/atoms/" "$json" POST 
}

function listMatches {
	user=$1
	server=$2
	atomId=$3
	operation="listing matches for atom ${atomId} for user $user on server $server"
	sendUserRequest "$operation" "$user" "$server" "/rest/atoms/${atomId}/matches/" "" GET 
}
	
function deleteCookies {	
	user=$1
	echo -n "deleting cookies for user $user"
	rm "${user}.cookies"
	reportSuccess $?
}

function cleanup {
	rm -f ${HTTP_RESPONSE_OUTPUT_FILE}
}



function extractAtomIdFromCreateResponse {
	response=$1
	echo "$response"
}

function assertHttpHeaderExists {
	headerName=$1
	echo -n " Checking if http http headers contains ${headerName} header"
	cat ${HTTP_RESPONSE_OUTPUT_FILE} | grep -q -e "^${headerName}:"
	success=$?
	reportSuccess $success
	if [[ $success -ne 0 ]]
	then
		echo " Did not find ${headerName} header in response:"
		cat ${HTTP_RESPONSE_OUTPUT_FILE}
	fi
	return $success
}

function assertHttpStatusCodeEquals {
	expectedCode=$1
	echo -n " Checking http status (expected: ${expectedCode})"
	cat ${HTTP_RESPONSE_OUTPUT_FILE} | grep -q "HTTP/1.1 ${expectedCode}" 
	success=$?
	reportSuccess $success
	if [[ $success -eq 0 ]] 
	then
		return 0
	else
		echo " Did not find http status code ${expectedCode} in response:"
		cat ${HTTP_RESPONSE_OUTPUT_FILE}
		return 1
	fi
}

function extractValueFromResponseJson {
	key=$1
	grep -Po "\"${key}\":\"?(.+?)\"?[,\]\}]" ${HTTP_RESPONSE_OUTPUT_FILE} | sed -re 's/^".+":"?//' | sed -re 's/"?.$//'
}

	
