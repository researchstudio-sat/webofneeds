#!/bin/bash
#create a user, try possible error cases
. data.sh
. rest-lib.sh

USER1=`createRandomString 8` 
PASSWORD1=`createRandomString 8` 
PASSWORD2=`createRandomString 8` 

createUser $USER1 $PASSWORD1 $PASSWORD2 $SERVER 400
signIn $USER1 $PASSWORD1 $SERVER 403
signIn $USER1 $PASSWORD2 $SERVER 403
signOut $USER1 $SERVER 200
#creation normally should be implemented according to http/1.1: 
#return status 201, "Location" header contains new URI. 
#With users, there is nor URI, however. Still, 201 should be the status
createUser $USER1 $PASSWORD1 $PASSWORD1 $SERVER 201 
createUser $USER1 $PASSWORD2 $PASSWORD2 $SERVER 409
signIn $USER1 $PASSWORD2 $SERVER 403
signOut $USER1 $SERVER 200 
signOut $USER1 $SERVER 200
signIn $USER1 $PASSWORD1 $SERVER 200
signIn $USER1 $PASSWORD1 $SERVER 200
signOut $USER1 $SERVER 200
signOut $USER1 $SERVER 200
deleteCookies $USER1
