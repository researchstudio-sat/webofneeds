Here, we cover the steps required to start a webofneeds system on a single machine.

These are the components:
* WoN node (webapp connected to an in-memory SQL database)
* Owner application (webapp connected to an in-memory SQL database, talking to the WoN node)
* Matching service (Akka application talking to an RDF database and the WoN node)
* Matcher implementation (SIREn/Solr instance talking to the matching service)
* RDF Database (needed by the matching service)

Basically, all these components need to be configured to know about each other and started. In addition to that, WoN node, owner application and matching service need (self-signed) certificates, which we'll also cover here.

# WoN node and Owner application
Follow the [instructions](https://github.com/researchstudio-sat/webofneeds/wiki/setting-up-the-web-apps) for running WoN node and owner app.

# Matching service
The matching service consists of a number of components: 
* the module for crawling 
* the module for subscribing to changes on WoN nodes
* the module for handling matching implementations
* the RDF database

## RDF database
Download it from the blazegraph website (https://www.blazegraph.com/download/) and follow their installation instructions - basically download the jar and run it:
```
java -server -Xmx4g -jar bigdata-bundled.jar
```
## SIREn
### SIREn Download & install
For providing live matching, a SIREn server is used.

Download the Apache Solr SIREn distribution (siren-solr-1.4-bin.zip) from the following link:
http://siren.solutions/siren/downloads/

Unpack the zip file.

The solr server in the example directory has a default core, `collection1`.
We need a specially prepared core, which can be found here:
`webofneeds/webofneeds/won-docker/sirensolr/core/won`. Copy the whole 'won' directory into the example/solr folder.

Then navigate to the example folder and run the server : 
```
java –jar start.jar 
```
SIREn now listens at http://localhost:8983/solr/#/
Further information is available in the [SIREn documentation](http://siren.solutions/manual/solr-getting-started.html)

## Main matching service logic

## SIREn based matcher implementation



[TODO]
