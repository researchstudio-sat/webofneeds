# Local won testing environment

This folder contains a docker-compose configuration for running integration tests based on the local build. 

The services therein are a complete won environment including matcher. 

# Running
Build the webofneeds project and run docker-compose:

```
cd ${clone-base} # won clone dir
cd webofneeds
mvn -Pskip-tests install
cd won-docker/deploy/local_image
docker-compose up -d
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