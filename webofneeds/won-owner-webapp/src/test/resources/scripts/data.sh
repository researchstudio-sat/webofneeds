#!/bin/bash
#create users with random 5-letter names
SERVER=http://localhost:8080/owner
JSON_CREATE_USER='{"username":"<NAME>", "password":"<PASSWORD1>", "passwordAgain":"<PASSWORD2>"}'
JSON_SIGN_IN='{"username":"<NAME>", "password":"<PASSWORD>"}'
JSON_SIGN_OUT='{"username":"<NAME>"}'
JSON_NEED_DEMAND_A='{"title":"couch", "textDescription":"I need a couch", "basicNeedType":"DEMAND", "state":"ACTIVE"}'
JSON_NEED_SUPPLY_A='{"title":"couch", "textDescription":"I need a couch", "basicNeedType":"SUPPLY", "state":"ACTIVE", "contentDescription":""}'
