# WoN Integration Tests

This module provides tests against a complete WoN environment (Node, Owner, Matcher). The environment is 
defined in a docker-compose project executed on a local docker server.

The docker-compose file used to start the environment is `/src/test/resources/docker-compose.yml`.
 
All tests inherit from `won.integrationtest.IntegrationTests`, the class that is responsible for starting the 
docker-compose project before the first test is run.

Container startup can be decoupled from running the tests, allowing you to start the containers manually once and 
then running the tests against them repeatedly, saving some 60 seconds each time. In order to do that, start 
the docker-compose project manually:
```
mvn test-compile
cd target/test-classes
docker-compose build
docker-compose up -d
```
And then pass the environment variable 
```
START_CONTAINERS=false
```
to the tests.

Note: after changing something in the node/owner/matcher code, be sure to build the module that was
changed, then `won-docker`, and then `won-integrationtest`. The reason is that a lot of data is copied
between these modules and you don't want to start a container with outdated resources/classes.

If you are working on node/owner/matcher code, it's recommended to use the docker-compose project in 
`won-docker/deploy/local_build` that has a lower turnaround time.  