#!/bin/bash
# creates two users with one need each. 
# the needs are expected to be matched by an external matching system 
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
createNeed $USER1 $SERVER "$JSON_NEED_DEMAND_A" 201
	assertHttpStatusCodeEquals 201
	USER1NeedId1=`extractValueFromResponseJson "needId"`
	USER1NeedUri1=`extractValueFromResponseJson "needURI"`
	echo " --> Created need ${USER1NeedId1} for user ${USER1}"
	
createUser $USER2 $PASSWORD2 $PASSWORD2 $SERVER
	assertHttpStatusCodeEquals 201
signIn $USER2 $PASSWORD2 $SERVER 200
	assertHttpStatusCodeEquals 200
createNeed $USER2 $SERVER "$JSON_NEED_SUPPLY_A" 201
	assertHttpStatusCodeEquals 201
	USER2NeedId1=`extractValueFromResponseJson "needId"`
	USER2NeedUri1=`extractValueFromResponseJson "needURI"`
	echo " --> Created need ${USER2NeedId1} for user ${USER2}"


listMatches $USER1 $SERVER $USER2NeedId1	
	assertHttpStatusCodeEquals 403
listMatches $USER2 $SERVER $USER1NeedId1	
	assertHttpStatusCodeEquals 403
sleep 5
listMatches $USER1 $SERVER $USER1NeedId1	
	assertHttpStatusCodeEquals 200
	USER1RemoteNeedUri1=`extractValueFromResponseJson "remoteNeedURI"`
	echo " --> Received hint on ${USER1NeedId1} for user ${USER1} to remote need ${USER1RemoteNeedUri1}"
listMatches $USER2 $SERVER $USER2NeedId1	
	assertHttpStatusCodeEquals 200
	USER2RemoteNeedUri1=`extractValueFromResponseJson "remoteNeedURI"`
	echo " --> Received hint on ${USER2NeedId1} for user ${USER2} to remote need ${USER2RemoteNeedUri1}"
	
deleteCookies $USER1
deleteCookies $USER2
cleanup