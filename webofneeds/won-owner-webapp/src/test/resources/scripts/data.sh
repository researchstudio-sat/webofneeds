#!/bin/bash
#create users with random 5-letter names
SERVER=http://localhost:8080/owner
JSON_CREATE_USER='{"username":"<NAME>", "password":"<PASSWORD1>"}'
JSON_SIGN_IN='{"username":"<NAME>", "password":"<PASSWORD>"}'
JSON_SIGN_OUT='{"username":"<NAME>"}'
JSON_ATOM_DEMAND_A='{"title":"couch", "textDescription":"I atom a couch", "basicAtomType":"DEMAND", "state":"ACTIVE"}'
JSON_ATOM_SUPPLY_A='{"title":"couch", "textDescription":"I atom a couch", "basicAtomType":"SUPPLY", "state":"ACTIVE", "contentDescription":""}'
