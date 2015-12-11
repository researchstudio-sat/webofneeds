# fail the whole script if one command fails
set -e

# build the won docker images on every server of the cluster so that everywhere is the latest version available
echo start docker build of images:

# wonnode
docker -H satsrv04:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv05:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv06:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv07:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/

# owner
docker -H satsrv04:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv05:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv06:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv07:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/

# matcher service
docker -H satsrv04:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv05:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv06:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv07:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/

# siren matcher
docker -H satsrv04:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv05:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv06:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv07:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/

# wonnode/owner server certificate generator
docker -H satsrv04:2375 build -t webofneeds/gencert:int $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv05:2375 build -t webofneeds/gencert:int $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv06:2375 build -t webofneeds/gencert:int $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv07:2375 build -t webofneeds/gencert:int $WORKSPACE/webofneeds/won-docker/gencert/

# bots
docker -H satsrv04:2375 build -t webofneeds/bots:int $WORKSPACE/webofneeds/won-docker/bots/
docker -H satsrv05:2375 build -t webofneeds/bots:int $WORKSPACE/webofneeds/won-docker/bots/
docker -H satsrv06:2375 build -t webofneeds/bots:int $WORKSPACE/webofneeds/won-docker/bots/
docker -H satsrv07:2375 build -t webofneeds/bots:int $WORKSPACE/webofneeds/won-docker/bots/


# start the won containers on dedicated servers of the cluster
echo run docker containers:

# postgres db 1
docker -H satsrv04:2375 pull webofneeds/postgres
docker -H satsrv04:2375 stop postgres_int || echo 'No docker container found to stop with name: postgres_int'
docker -H satsrv04:2375 rm postgres_int || echo 'No docker container found to remove with name: postgres_int'
docker -H satsrv04:2375 run --name=postgres_int -d -p 5433:5432 webofneeds/postgres

# postgres db 2
docker -H satsrv05:2375 pull webofneeds/postgres
docker -H satsrv05:2375 stop postgres_int || echo 'No docker container found to stop with name: postgres_int'
docker -H satsrv05:2375 rm postgres_int || echo 'No docker container found to remove with name: postgres_int'
docker -H satsrv05:2375 run --name=postgres_int -d -p 5433:5432 webofneeds/postgres

sleep 10

# wonnode/owner server certificate generator
# Please note that value of PASS should be the same used in your server.xml for SSLPassword on wonnode and owner,
# and the same as activemq.broker.keystore.password used in your wonnode activemq spring configurations for broker
docker -H satsrv04:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv04:2375 run --name=gencert_int -e CN="satsrv04.researchstudio.at" -e "PASS=changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:int
docker -H satsrv05:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv05:2375 run --name=gencert_int -e CN="satsrv05.researchstudio.at" -e "PASS=changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:int
docker -H satsrv06:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv06:2375 run --name=gencert_int -e CN="satsrv06.researchstudio.at" -e "PASS=changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:int
docker -H satsrv07:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv07:2375 run --name=gencert_int -e CN="satsrv07.researchstudio.at" -e "PASS=changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:int


sleep 5

#stop won _int instances
docker -H satsrv04:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv04:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv05:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv05:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'


# wonnode 1
docker -H satsrv04:2375 stop wonnode_int1 || echo 'No docker container found to stop with name: wonnode_int1'
docker -H satsrv04:2375 rm wonnode_int1 || echo 'No docker container found to remove with name: wonnode_int1'
docker -H satsrv04:2375 run --name=wonnode_int1 -d -e "uri.host=satsrv04.researchstudio.at" -e "http.port=8889" -e \
"activemq.broker.port=61617" -p 8889:8443 -p 61617:61617 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/wonnode_int1:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5433/won_node1" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "monitoring.output.dir=/usr/local/tomcat/won" \
-e "monitoring.interval.seconds=60" \
-p 9010:9010 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=satsrv04.researchstudio.at" \
-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/temp/" \
-m 350m -v /home/install/hdumps/node-int1:/usr/local/tomcat/temp/ \
webofneeds/wonnode:int

# wonnode 2
docker -H satsrv05:2375 stop wonnode_int2 || echo 'No docker container found to stop with name: wonnode_int2'
docker -H satsrv05:2375 rm wonnode_int2 || echo 'No docker container found to remove with name: wonnode_int2'
docker -H satsrv05:2375 run --name=wonnode_int2 -d -e "uri.host=satsrv05.researchstudio.at" -e "http.port=8889" \
-e "activemq.broker.port=61617" -p 8889:8443 -p 61617:61617 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/wonnode_int2:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5433/won_node2" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "monitoring.output.dir=/usr/local/tomcat/won" \
-e "monitoring.interval.seconds=60" \
-p 9010:9010 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=satsrv05.researchstudio.at" \
-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/temp/" \
-m 350m -v /home/install/hdumps/node-int:/usr/local/tomcat/temp/ \
webofneeds/wonnode:int

# wonnode 3
docker -H satsrv04:2375 stop wonnode_int3 || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv04:2375 rm wonnode_int3 || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv04:2375 run --name=wonnode_int3 -d -e "uri.host=satsrv04.researchstudio.at" -e "http.port=8890" -e \
"activemq.broker.port=61618" -p 8890:8443 -p 61618:61618 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/wonnode_int3:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5433/won_node3" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "monitoring.output.dir=/usr/local/tomcat/won" \
-e "monitoring.interval.seconds=60" \
-p 9012:9012 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9012 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9012 -Djava.rmi.server.hostname=satsrv04.researchstudio.at" \
-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/temp/" \
-m 350m -v /home/install/hdumps/node-int3:/usr/local/tomcat/temp/ \
webofneeds/wonnode:int

# wonnode 4
docker -H satsrv05:2375 stop wonnode_int4 || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv05:2375 rm wonnode_int4 || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv05:2375 run --name=wonnode_int4 -d -e "uri.host=satsrv05.researchstudio.at" -e "http.port=8890" \
-e "activemq.broker.port=61618" -p 8890:8443 -p 61618:61618 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/wonnode_int4:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5433/won_node4" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "monitoring.output.dir=/usr/local/tomcat/won" \
-e "monitoring.interval.seconds=60" \
-p 9012:9012 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9012 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9012 -Djava.rmi.server.hostname=satsrv05.researchstudio.at" \
-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/temp/" \
-m 350m -v /home/install/hdumps/node-int4:/usr/local/tomcat/temp/ \
webofneeds/wonnode:int


#sleep 20

# owner 1
docker -H satsrv04:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv04:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
docker -H satsrv04:2375 run --name=owner_int -d -e "node.default.host=satsrv04.researchstudio.at" \
-e "node.default.http.port=8889" -p 8082:8443 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/owner_int:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-p 9011:9011 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=satsrv04.researchstudio.at" \
-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/temp/mem-err.hprof" \
-m 350m -v /home/install/hdumps/owner-int:/usr/local/tomcat/temp/ \
webofneeds/owner:int

# owner 2
docker -H satsrv05:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv05:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
docker -H satsrv05:2375 run --name=owner_int -d -e "node.default.host=satsrv05.researchstudio.at" \
-e "node.default.http.port=8889" -p 8082:8443 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/owner_int:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-p 9011:9011 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=satsrv05.researchstudio.at" \
-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/temp/mem-err.hprof" \
-m 350m -v /home/install/hdumps/owner-int:/usr/local/tomcat/temp/ \
webofneeds/owner:int

# bigdata
docker -H satsrv06:2375 pull webofneeds/bigdata
docker -H satsrv06:2375 stop bigdata_int || echo 'No docker container found to stop with name: bigdata_int'
docker -H satsrv06:2375 rm bigdata_int || echo 'No docker container found to remove with name: bigdata_int'
docker -H satsrv06:2375 run --name=bigdata_int -d -p 10000:9999 \
-m 256m webofneeds/bigdata

# matcher service
docker -H satsrv06:2375 stop matcher_service_int || echo 'No docker container found to stop with name: matcher_service_int'
docker -H satsrv06:2375 rm matcher_service_int || echo 'No docker container found to remove with name: matcher_service_int'
docker -H satsrv06:2375 run --name=matcher_service_int -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" \
-v /home/install/won-client-certs/matcher_service_int:/usr/src/matcher-service/client-certs/ \
-e "uri.sparql.endpoint=http://satsrv06.researchstudio.at:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://satsrv04.researchstudio.at:8889/won/resource,https://satsrv04.researchstudio.at:8890/won/resource,https://satsrv05.researchstudio.at:8889/won/resource,https://satsrv05.researchstudio.at:8890/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -e "matcher.service.monitoring=true" \
-p 2561:2561 \
-p 9010:9010 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/temp/mem-err.hprof" \
-m 350m -v /home/install/hdumps/matcher-service-int:/usr/local/temp/ \
webofneeds/matcher_service:int

# siren solr server
docker -H satsrv06:2375 pull webofneeds/sirensolr
docker -H satsrv06:2375 stop sirensolr_int || echo 'No docker container found to stop with name: sirensolr_int'
docker -H satsrv06:2375 rm sirensolr_int || echo 'No docker container found to remove with name: sirensolr_int'
docker -H satsrv06:2375 run --name=sirensolr_int -d -p 7071:8080 -p 8984:8983 \
-p 9012:9012 \
-e CATALINA_OPTS="-Xmx200m  -XX:MaxPermSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/temp/mem-err.hprof -Dcom.sun.management.jmxremote.port=9012 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9012 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-m 350m webofneeds/sirensolr

sleep 20

# siren matcher
docker -H satsrv06:2375 stop matcher_siren_int || echo 'No docker container found to stop with name: matcher_siren_int'
docker -H satsrv06:2375 rm matcher_siren_int || echo 'No docker container found to remove with name: matcher_siren_int'
docker -H satsrv06:2375 run --name=matcher_siren_int -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-e "matcher.siren.uri.solr.server=http://satsrv06.researchstudio.at:8984/solr/won/" \
-e "matcher.siren.uri.solr.server.public=http://satsrv06.researchstudio.at:8984/solr/#/won/" \
-p 9011:9011 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx190m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/temp/mem-err.hprof" \
-m 350m -v /home/install/hdumps/matcher-siren-int:/usr/local/temp/ \
-p 2562:2562 webofneeds/matcher_siren:int


sleep 20

docker -H satsrv06:2375 stop need_creator_bot_int || echo 'No docker container found to stop with name: need_creator_bot_int'
docker -H satsrv06:2375 rm need_creator_bot_int || echo 'No docker container found to remove with name: need_creator_bot_int'
docker -H satsrv06:2375 run --name=need_creator_bot_int -d \
-e "node.default.host=satsrv04.researchstudio.at" -e "node.default.http.port=8889" \
-e "won.node.uris=https://satsrv04.researchstudio.at:8889/won/resource https://satsrv05.researchstudio.at:8889/won/resource https://satsrv04.researchstudio.at:8890/won/resource https://satsrv05.researchstudio.at:8890/won/resource" \
-e "mail.directory.supply=/usr/src/mails/supply" \
-e "mail.directory.demand=/usr/src/mails/demand" \
-e "needCreatorBot.period=500" \
-p 9013:9013 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9013 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9013 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx150m  -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/temp/mem-err.hprof" \
-e "MAIN_BOT=won.bot.app.NeedCreatorBotApp" \
-v /home/install/freecycle:/usr/src/mails/ \
-m 300m -v /home/install/hdumps/need-creator-bot-int:/usr/local/temp/ \
webofneeds/bots:int
