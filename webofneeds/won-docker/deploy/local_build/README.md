# Local won environment

This folder contains a docker-compose configuration. The services therein are 
a complete won environment including matcher and debugbot. `won-node-webapp` and `won-owner-webapp` automatically load the contents of `target/classes` of their `won-*` dependencies. No need to build any jars or wars, just restart the container.

Debugger ports:
* won-owner-webapp: 62000
* won-node-webapp: 62001 

## Preparation

 
Build the webofneeds project (the fast way is probably desired):
```
cd ${clone-base}/webofneeds/webofneeds
mvn -Pskip-tests install
```

When starting for the first time, run the `deploy.sh`bash script:
It will create a volume for the mongodb and postgres container (required)
```
./deploy.sh
```

## Starting the Environment
Assuming the webofneeds project has been built (the `target/classes` folders are populated with up-to-date classes):

Either using `deploy.sh` (see above) or explicitly using docker-compose commands. In this case, you
need to first set up environment variables using `setenv.sh`. This file must be sourced (not executed, ie.)

```
. setenv.sh
docker-compose up -d
```

## Hints
### Fast Reload Cycles
With this config, the best option with standard tooling is the following
1. Attach the debugger and use the hot-deployment feature. Works as long as you don't change method signatures and you don't need any spring magic to re-initialize your beans.
2. If you have to restart your spring context, (i.e. option 1 is not enough) it's enough to run `mvn compile` in all modules that need updating and then restart the affected docker-compose service.

For example, if you made changes to a spring-xml file (I know...) in `won-node`, do this:
```
cd won-node
mvn compile
docker-compose restart wonnode
```  


### Docker Sharing options 
* Under Windows, docker isn't very stable when sharing many directories. It's easier to just share the whole webofneeds direcory with docker than to share the individual mounts.
### Keeping data across restarts
* By default, mongodb, postgres and bigdata use volumes and keep their data. This can be disabled by commenting out the `volumes` keys in docker-compose.yml
NOTE: there is only *one* postgres volume, even if you run this environment in multiple base folders. 