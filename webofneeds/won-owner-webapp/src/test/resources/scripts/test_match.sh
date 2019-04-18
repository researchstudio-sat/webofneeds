#!/bin/bash
# creates two users with one atom each. 
# the atoms are expected to be matched by an external matching system 
. data.sh
. rest-lib.sh

USER1=`createRandomString 8` 
PASSWORD1=`createRandomString 8` 
USER2=`createRandomString 8`
PASSWORD2=`createRandomString 8` 

createUser $USER1 $PASSWORD1 $PASSWORD1 $SERVER
	assertHttpStatusCodeEquals 201
signIn $USER1 $PASSWORD1 $SERVER 200
	assertHttpStatusCodeEquals 200
createAtom $USER1 $SERVER "$JSON_ATOM_DEMAND_A" 201
	assertHttpStatusCodeEquals 201
	USER1AtomId1=`extractValueFromResponseJson "atomId"`
	USER1AtomUri1=`extractValueFromResponseJson "atomURI"`
	echo " --> Created atom ${USER1AtomId1} for user ${USER1}"
	
createUser $USER2 $PASSWORD2 $PASSWORD2 $SERVER
	assertHttpStatusCodeEquals 201
signIn $USER2 $PASSWORD2 $SERVER 200
	assertHttpStatusCodeEquals 200
createAtom $USER2 $SERVER "$JSON_ATOM_SUPPLY_A" 201
	assertHttpStatusCodeEquals 201
	USER2AtomId1=`extractValueFromResponseJson "atomId"`
	USER2AtomUri1=`extractValueFromResponseJson "atomURI"`
	echo " --> Created atom ${USER2AtomId1} for user ${USER2}"


listMatches $USER1 $SERVER $USER2AtomId1	
	assertHttpStatusCodeEquals 403
listMatches $USER2 $SERVER $USER1AtomId1	
	assertHttpStatusCodeEquals 403
sleep 5
listMatches $USER1 $SERVER $USER1AtomId1	
	assertHttpStatusCodeEquals 200
	USER1TargetAtomUri1=`extractValueFromResponseJson "targetAtomURI"`
	echo " --> Received hint on ${USER1AtomId1} for user ${USER1} to remote atom ${USER1TargetAtomUri1}"
listMatches $USER2 $SERVER $USER2AtomId1	
	assertHttpStatusCodeEquals 200
	USER2TargetAtomUri1=`extractValueFromResponseJson "targetAtomURI"`
	echo " --> Received hint on ${USER2AtomId1} for user ${USER2} to remote atom ${USER2TargetAtomUri1}"
	
deleteCookies $USER1
deleteCookies $USER2
cleanup