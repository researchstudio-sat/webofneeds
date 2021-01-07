# Local won development environment

This folder contains a docker-compose configuration intended for use while developing. The services therein are 
a complete won environment including matcher. 

`won-node-webapp` and `won-owner-webapp` automatically load the contents of `target/classes` of their `won-*` dependencies. No need to build any jars or wars, just restart the container using
```
 docker-compose stop wonnode owner && docker-compose up -d wonnode owner
```

Application links:
* [owner webapp at https://localhost:8082/owner](https://localhost:8082/owner)
* [node webapp at https://localhost:8443/won/resource](https://localhost:8443/won/resource)

Debugger ports:
* won-node-webapp: 62000 
* won-owner-webapp: 62010

JMX ports:
* won-node-webapp: 61000
* won-owner-webapp: 61010

If you want another owner app or bot to connect: it will only work if this link works (see 'Accessing the WoN Node' below)

[node webapp at https://wonnode:8443/won](https://wonnode:8443/won)

## Preparation
 
Build the webofneeds project (the fast way is probably desired):

```
cd ${clone-base} # the directory the project was cloned into
cd webofneeds
mvn -Pskip-tests install
```

When starting for the first time, run the `deploy.sh`bash script:
It will create a volume for the mongodb and postgres container (required)

```
cd ${clone-base} # the directory the project was cloned into
cd webofneeds/won-docker/deploy/local_build
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

## Accessing the WoN Node

Inside the docker-compose project, the node's hostname is `wonnode`. For browsing, you can reach it under [https://localhost:8443/won/resource](https://localhost:8443/won/resource) because the port is mapped to the docker host. However, if you want to connect another owner app or bot, you have to tell your system how to resolve `wonnode`:

Add this to your `etc/hosts` (or `C:\windows\system32\drivers\etc\hosts` in windows):
```
127.0.0.1       wonnode
``` 

Then, you can start e.g. the [won-debugbot](https://github.com/researchstudio-sat/won-debugbot/) using the following setting:
```
export WON_NODE_URI="https://wonnode:8443/won"
java -jar target/bot.jar
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

### Docker is slow on Windows?

Under Windows it's common for docker to be super slow. If it happens to you:
* Make sure your shared directories are not being virus scanned!
* You may want to try [docker-sync](https://docker-sync.readthedocs.io/en/latest/index.html). If you do, let us know how that went! 
* You are not alone: https://github.com/docker/for-win/issues/1936

### Keeping data across restarts

* By default, mongodb, postgres and bigdata use volumes and keep their data. This can be disabled by commenting out the `volumes` keys in docker-compose.yml
NOTE: there is only *one* postgres volume, even if you run this environment in multiple base folders. 