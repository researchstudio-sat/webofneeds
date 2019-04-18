#!/bin/bash
#create a user and sign her in
. data.sh
. rest-lib.sh

USER1=`createRandomString 8` 
PASSWORD1=`createRandomString 8` 

createUser $USER1 $PASSWORD1 $PASSWORD1 $SERVER
	assertHttpStatusCodeEquals 201
signIn $USER1 $PASSWORD1 $SERVER 200
	assertHttpStatusCodeEquals 200
createAtom $USER1 $SERVER "$JSON_ATOM_DEMAND_A" 201
	assertHttpStatusCodeEquals 201
	assertHttpHeaderExists "Location" 
	extractValueFromResponseJson "atomId"
deleteCookies $USER1
cleanup