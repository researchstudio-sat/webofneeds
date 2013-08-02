#!/bin/sh
WON="http://purl.org/webofneeds/model#"
RDFS="http://www.w3.org/2000/01/rdf-schema#"
SIREN="http://ec2-176-34-213-201.eu-west-1.compute.amazonaws.com:8080/siren"
java -Djava.util.logging.config.file=logconf/logging.properties \
	-jar ldspider.jar \
	-b 10  -s seed.txt \
	-osi ${SIREN} \
	-p data \
	-f "${RDFS}member" -f "${WON}belongsToNeed" \
	-f "${WON}hasRemoteConnection" -f "${WON}hasConnections" \
	1> ldspider.log 2> ldspider.log


