#/bin/sh
#create users with random 5-letter names
USER1=`cat /dev/urandom | tr -dc 'a-zA-Z' | fold -w 5 | head -n 1` 
USER2=`cat /dev/urandom | tr -dc 'a-zA-Z' | fold -w 5 | head -n 1`
SERVER=http://localhost:8080/owner

JSON_CREATE_USER='{"username":"<NAME>", "password":"abc"}'
JSON_SIGN_IN='{"username":"<NAME>", "password":"abc"}'
JSON_ATOM_DEMAND_A='{"title":"couch", "textDescription":"I atom a couch", "basicAtomType":"DEMAND"}'
JSON_ATOM_SUPPLY_A='{"title":"couch", "textDescription":"I atom a couch", "basicAtomType":"SUPPLY"}'

function outputResponseIfNotOk {
	output=$1
	echo $output | grep -q "HTTP/1.1 200 OK" 
	if [[ $? ]] 
	then
		return 0
	else
		echo "got not ok answer:"
		echo $output
		return 1
	fi
}

function reportSuccess {
	operation=$1
	success=$2
	if [[ success ]]
	then
		echo ": succeeded"
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
	echo -n $operation
	output=`curl -i -X ${method} \
		-H 'Content-Type: application/json' \
		-d ''"$json"'' \
		-c ${user}.cookies \
		-b ${user}.cookies \
		${server}$path 2>/dev/null`
	outputResponseIfNotOk "$output"
	reportSuccess "$operation" $?
	if $returnOutput 
	then
		echo $output
	fi
}


function createUser {
	user=$1
	server=$2
	operation="creating user $user on server $server"
	json="${JSON_CREATE_USER/<NAME>/$user}"
	sendUserRequest "$operation" "$user" "$server" "/rest/users/" "$json" POST false
}

function signIn {		
	user=$1
	server=$2
	operation="signing user $user in on server $server"
	#sign in
	json="${JSON_CREATE_USER/<NAME>/$user}"
	sendUserRequest "$operation" "$user" "$server" "/rest/users/signin/" "$json" POST false
}

function createAtom {
	#post an atom
	operation="creating an atom for user $user on server $server"
	user=$1
	server=$2
	json=$3
	output=`sendUserRequest "$operation" "$user" "$server" "/rest/atoms/" "$json" POST true`
	echo $output
}

function listMatches {
	#post an atom
	operation="creating an atom for user $user on server $server"
	user=$1
	server=$2
	json=$3
	sendUserRequest "$operation" "$user" "$server" "/rest/atoms/" "$json" POST	
}
	
function deleteCookies {	
	user=$1
	operation="deleting cookies for user $user"
	rm ${user}.cookies	
	reportSuccess "$operation" $?
}

function cleanup {
	deleteCookies $USER1
	deleteCookies $USER2
}

createUser $USER1 $SERVER
signIn $USER1 $SERVER
createUser $USER2 $SERVER
signIn $USER2 $SERVER
createAtom $USER1 $SERVER $JSON_ATOM_DEMAND_A
createAtom $USER2 $SERVER $JSON_ATOM_SUPPLY_A
cleanup




	
