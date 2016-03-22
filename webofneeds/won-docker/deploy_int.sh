# fail the whole script if one command fails
set -e

# build the won docker images on every server of the cluster so that everywhere is the latest version available
echo start docker build of images:

# wonnode 1
docker -H satsrv04:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv05:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv06:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv07:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/

# owner 1
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
docker -H satsrv04:2375 run --name=postgres_int -d -p 5433:5432 -m 256m webofneeds/postgres

# postgres db 2
docker -H satsrv05:2375 pull webofneeds/postgres
docker -H satsrv05:2375 stop postgres_int || echo 'No docker container found to stop with name: postgres_int'
docker -H satsrv05:2375 rm postgres_int || echo 'No docker container found to remove with name: postgres_int'
docker -H satsrv05:2375 run --name=postgres_int -d -p 5433:5432 -m 256m webofneeds/postgres

sleep 10

# wonnode/owner server certificate generator
# Please note that value of PASS (if you set a non-default password) should be the same used in your server.xml for
# SSLPassword on wonnode and owner, and the same as activemq.broker.keystore.password used in your wonnode activemq
# spring configurations for broker, set the password with "-e PASS=pass:<your_password>" or "-e
# PASS=file:<your_file_with_password>"
docker -H satsrv04:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv04:2375 run --name=gencert_int -e CN="satsrv04.researchstudio.at" -e "PASS=pass:changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:int
docker -H satsrv05:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv05:2375 run --name=gencert_int -e CN="satsrv05.researchstudio.at" -e "PASS=pass:changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:int
docker -H satsrv06:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv06:2375 run --name=gencert_int -e CN="satsrv06.researchstudio.at" -e "PASS=pass:changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:int
docker -H satsrv07:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv07:2375 run --name=gencert_int -e CN="satsrv07.researchstudio.at" -e "PASS=pass:changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:int


sleep 5

#stop wonnode_int1/2/3/4 instances
docker -H satsrv04:2375 stop wonnode_int1 || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv04:2375 rm wonnode_int1 || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv04:2375 stop wonnode_int3 || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv04:2375 rm wonnode_int3 || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv05:2375 stop wonnode_int2 || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv05:2375 rm wonnode_int2 || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv05:2375 stop wonnode_int4 || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv05:2375 rm wonnode_int4 || echo 'No docker container found to remove with name: wonnode_int'


# wonnode 1
docker -H satsrv04:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv04:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv04:2375 run --name=wonnode_int -d -e "uri.host=satsrv04.researchstudio.at" -e "http.port=8889" -e \
"activemq.broker.port=61617" -p 8889:8443 -p 61617:61617 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/wonnode_int:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-p 9010:9010 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=satsrv04.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
webofneeds/wonnode:int

# wonnode 2
docker -H satsrv05:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv05:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv05:2375 run --name=wonnode_int -d -e "uri.host=satsrv05.researchstudio.at" -e "http.port=8889" \
-e "activemq.broker.port=61617" -p 8889:8443 -p 61617:61617 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/wonnode_int:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-p 9010:9010 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=satsrv05.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
webofneeds/wonnode:int

sleep 20

# expect OWNER won-mail-sender host, user and password (i.e. configuration for no-replay won-owner-app-email-account) be
# set as environment variables, e.g. MAIL_USER=changeuser MAIL_PASS=changepass MAIL_HOST=smtp.changehost.com
echo ${MAIL_USER} at ${MAIL_HOST} is used as owner no-replay won-owner-app-email-account

# owner 1
docker -H satsrv04:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv04:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
docker -H satsrv04:2375 run --name=owner_int -d -e "node.default.host=satsrv04.researchstudio.at" \
-e "uri.host=satsrv04.researchstudio.at" -e "http.port=8082" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-e "node.default.http.port=8889" -p 8082:8443 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/owner_int:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-p 9011:9011 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=satsrv04.researchstudio.at" \
-e "JMEM_OPTS=-Xmx1000m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/owner:int

# owner 2
docker -H satsrv05:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv05:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
docker -H satsrv05:2375 run --name=owner_int -d -e "node.default.host=satsrv05.researchstudio.at" \
-e "uri.host=satsrv05.researchstudio.at" -e "http.port=8082" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-e "node.default.http.port=8889" -p 8082:8443 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/owner_int:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-p 9011:9011 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=satsrv05.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
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
-e "uri.sparql.endpoint=http://satsrv06.researchstudio.at:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://satsrv04.researchstudio.at:8889/won/resource,https://satsrv05.researchstudio.at:8889/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -p 2561:2561 \
-v /home/install/won-client-certs/matcher_service_int:/usr/src/matcher-service/client-certs/ \
-p 9010:9010 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
webofneeds/matcher_service:int

# siren solr server
docker -H satsrv06:2375 pull webofneeds/sirensolr
docker -H satsrv06:2375 stop sirensolr_int || echo 'No docker container found to stop with name: sirensolr_int'
docker -H satsrv06:2375 rm sirensolr_int || echo 'No docker container found to remove with name: sirensolr_int'
docker -H satsrv06:2375 run --name=sirensolr_int -d -p 7071:8080 -p 8984:8983 \
-p 9012:9012 \
-e CATALINA_OPTS="-Xmx200m  -XX:MaxPermSize=150m -XX:+HeapDumpOnOutOfMemoryError -Dcom.sun.management.jmxremote.port=9012 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9012 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-m 350m webofneeds/sirensolr

sleep 10

# siren matcher
docker -H satsrv06:2375 stop matcher_siren_int || echo 'No docker container found to stop with name: matcher_siren_int'
docker -H satsrv06:2375 rm matcher_siren_int || echo 'No docker container found to remove with name: matcher_siren_int'
docker -H satsrv06:2375 run --name=matcher_siren_int -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-e "matcher.siren.uri.solr.server=http://satsrv06.researchstudio.at:8984/solr/won/" \
-e "matcher.siren.uri.solr.server.public=http://satsrv06.researchstudio.at:8984/solr/#/won/" \
-p 9011:9011 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
-p 2562:2562 webofneeds/matcher_siren:int


sleep 20
docker -H satsrv06:2375 stop need_creator_bot_int || echo 'No docker container found to stop with name: need_creator_bot_int'
docker -H satsrv06:2375 rm need_creator_bot_int || echo 'No docker container found to remove with name: need_creator_bot_int'
docker -H satsrv06:2375 stop echo_bot_int || echo 'No docker container found to stop with name: echo_bot_int'
docker -H satsrv06:2375 rm echo_bot_int || echo 'No docker container found to remove with name: echo_bot_int'
docker -H satsrv06:2375 stop debug_bot_int || echo 'No docker container found to stop with name: debug_bot_int'
docker -H satsrv06:2375 rm debug_bot_int || echo 'No docker container found to remove with name: debug_bot_int'
docker -H satsrv06:2375 run --name=debug_bot_int -d \
-e "node.default.host=satsrv04.researchstudio.at" -e "node.default.http.port=8889" \
-e "won.node.uris=https://satsrv04.researchstudio.at:8889/won/resource https://satsrv05.researchstudio.at:8889/won/resource" \
-p 9013:9013 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9013 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9013 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 300m \
webofneeds/bots:int
