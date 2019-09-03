# Monitoring Docker Containers
## Remote monitoring

You can monitor wonnode, owner, matcher and some other web of needs docker containers using JavaVisualVM via JMX ports. See example of exposing JMX ports in `webofneeds/webofneeds/won-docker/deploy_int_loadtest.sh`.

For example, wonnode can be run with the 2 following additional parameters:

`-p 9010:9010`

`-e JMX_OPTS=-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=example.server.at"`

This makes it possible to run locally `JavaVisualVM` application (e.g. `C:\Program Files\Java\jdk1.8.0_45\bin\jvisualvm.exe`), add remote server via the application GUI (in the example it is `example.server.at` that would be added), specify JMX port (in the example it is `9010`), and start monitoring memory consumption, threads, etc.

If you install `MBeans` plugin for the `JavaVisualVM`, you can also monitor activemq broker messages flow.


## View logs

To view logs of the container, use docker logs command. E.g. to view the logs of the container with name `wonnode_dev`, do:

`docker -H localhost:2375 logs wonnode_dev | less`

Built by default webofneeds images have INFO level, but if your build images yourself you can specify other log level (point to another log file configuration) in your `Dockerfile` files.


## Restart signle won container
If a container (e.g. wonnode_int container) stopped (e.g. due to memory error, or you stopped it youself), you can run it again

with the same as before parameters:

`docker -H localhost:2375 restart wonnode_int`

with different parameters:

`docker -H localhost:2375 rm wonnode_int`

`docker -H localhost:2375 run --name=wonnode_int -d [PARAMETERS]`

Parameters used with won containers can be found in `webofneeds/webofneeds/won-docker/deploy*.sh`

## Adjust memory for container
We can adjust memory setting for Java and for docker container. Java memory can be provided when container is run via JMEM_OPTS variable.  Container memory is limited with -m option (important to limit otherwise docker will consume all the memory and hang the server). For example, on a small server wonnode can be run with the 2 following additional parameters:

`-m 350m`

`-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError"`

The +HeapDumpOnOutOfMemoryError takes care of creating a memory dump file when a memory error occurs which can be loaded into the Java VisualVM and analysed. The dump is stored inside the container - the logs show the exact path. To copy it from outside the container (e.g. from wonnode_int to /home/temp/ directory) do:

`docker -H localhost:2375 cp wonnode_int:/usr/local/tomcat/java_pid1.hprof temp/`
