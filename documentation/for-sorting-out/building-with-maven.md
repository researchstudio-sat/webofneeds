# Requirements

* Maven version > 3.2 (we use 3.3.3; setups with 3.5 seem to encounter problems)
* JDK version > 1.8  

# Building

To build the project, go to the webofneeds folder and type

`mvn install`


This will build the whole project, including some larger artifacts that you may or may not need. To skip building all of those, use 

`mvn install -P skip-frontend,skip-matcher-uberjar,skip-bot-uberjar,skip-matcher-rescal-uberjar,skip-node-webapp-war,skip-owner-webapp-war,skip-tests`

Details:
* `/webofneeds/won-node-webapp/target/won-node-webapp-[version].war`: the web application for the WoN node (the linked data server and communication node)
  * skip this using the maven profile 'skip-node-webapp-war'
  * skip building the war file and just exploding it in the target folder with the maven profile 'skip-node-webapp-war-but-explode'
* `webofneeds/won-owner-webapp/won-owner-webapp-[version].war`: the user-facing application that talks to WoN nodes in the background
  * skip the complete 'frontend' install and generation (i.e. npm install and resource munging with gulp) before building the webapp with the maven profile 'skip-frontend'
  * skip the 'frontend' install but do generation (i.e. resource munging with gulp) before building the webapp with the maven profile 'skip-frontend-all-but-gulp'
  * skip building the webapp using the maven profile 'skip-owner-webapp-war'
  * skip building the war file and just exploding the webapp in the target folder with the maven profile 'skip-owner-webapp-war-but-explode'
* `/webofneeds/won-bot/target/bots.jar`: a jar file that includes all the necessary dependencies for running some [Bot](/webofneeds/won-bot/README.md) implementations.
  * skip this using the maven profile 'skip-bot-uberjar'
* `/webofneeds/won-matcher-service/target/won-matcher-service.jar`: a jar file that includes all the necessary dependencies for running the main matching service
  * skip this using the maven profile 'skip-matcher-uberjar'
* `/webofneeds/won-matcher-rescal/target/won-matcher-rescal.jar`: a jar file that includes all the necessary dependencies for running a [RESCAL](https://github.com/nzhiltsov/Ext-RESCAL) based matcher.
  * skip this using the maven profile 'skip-matcher-rescal-uberjar'

HINT: there may be problems with some maven versions, choose the newest maven version. maven 3.3.3 is working for us. 


# Other maven profiles
In addition to the profiles mentioned above, there are several profiles available for building in Maven:
* `copy-module-dependencies` - copies dependencies to `<module-dir>/target/copiedDependencies` in lifecycle phase `validate`
* `copy-project-dependencies` - copies dependencies to `<exec-dir>/target/copiedDependencies` in lifecycle phase `validate`
* `skip-dependencies` - sets many dependencies' scopes to `provided`. Causes faster builds with smaller war files.
* `no-warn` - suppresses warnings
* `skip-tests` - skips the tests

# Running the services
Follow the guide in the [`won-docker README`](../webofneeds/won-docker/README.md).


# Speeding up server startup
At startup, the jasper compiler seems to scan all jar files that are not explicitly excluded for tag library descriptors. We don't really need this, as we don't use JSPs. Therefore, all our common libs should be excluded from scanning. A list of these files can be found in 
```
/toolconfig/tomcat/tomcat-jasper-scan-exclude-jars.txt
```
Find the `catalina.properties` file for your deployment and add the list to the values for config property

`tomcat.util.scan.DefaultJarScanner.jarsToSkip` 
and
`org.apache.catalina.startup.TldConfig.jarsToSkip`
