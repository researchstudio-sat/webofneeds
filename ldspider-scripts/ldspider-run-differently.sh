#!/bin/sh
# feel free to change this script for testing purposes. It's not part of any routine
WON="http://purl.org/webofneeds/model#"
RDFS="http://www.w3.org/2000/01/rdf-schema#"
java -jar ldspider-trunk.jar -c 50 -s seed.txt \
	-oe http://sat017:8890/sparql \
	-f "${RDFS}member" -f "${WON}belongsToNeed" \
	-f "${WON}hasRemoteConnection" -f "${WON}hasConnections"


