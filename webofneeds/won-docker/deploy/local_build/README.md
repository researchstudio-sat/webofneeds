# Local won environment

This folder contains a docker-compose configuration. The services therein are 
a complete won environment including matcher and debugbot. `won-node-webapp` and `won-owner-webapp` automatically load the contents of `target/classes` of their `won-*` dependencies. No need to build any jars or wars, just restart the container. 

## Preparation

 
Build the webofneeds project (the fast way is probably desired):
```
cd ${clone-base}/webofneeds/webofneeds
mvn -Pskip-tests install
```

Prepare the postgres volume and env var:
```
./setup-postgres-volume.sh
source ./setenv.sh
```


## Starting the Environment
Assuming the webofneeds project has been built (the `target/classes` folders are populated with up-to-date classes):
```
docker-compose up -d
```

## Hints
### Docker Sharing options 
* Under Windows, docker isn't very stable when sharing many directories. It's easier to just share the whole webofneeds direcory with docker than to share the individual mounts.
### Keeping data across restarts
* By default, postgres and bigdata use volumes and keep their data. This can be disabled by commenting out the `volumes` keys in docker-compose.yml