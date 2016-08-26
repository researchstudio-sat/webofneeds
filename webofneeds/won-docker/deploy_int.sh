#############################################################################################################
# Deployment descriptions:
# This script deploys several docker containers of the won application of the following servers:
#
# - create server certificate for satsrv04
# - create server certificate for satsrv05 and satsrv06
# - start nginx proxy server on satsrv06 for owner and wonnode on host satsrv05
# - start portsgres need databases for owner and wonnode on satsrv04 and satsrv05
# - start wonnode on satsrv04 (without proxy) => https://satsrv04:8889/won
# - start wonnode on satsrv05 (with proxy on satsrv06) => https://satsrv06/won
# - start owner on satsrv04 (without proxy) => https://satsrv04:8082/owner
# - start owner on satsrv05 (with proxy on satsrv06) => https://satsrv06/
# - start bigdata rdf store on satsrv06 for matcher service
# - start matcher service on satsrv06 and connect with wonnodes on satsrv04 and proxied wonnode on satsrv05
# - start solr server on satsrv06 as a need index
# - start solr matcher on satsrv06 as a matcher and connect to matcher service
# - start debug bot on satsrv06
##############################################################################################################

# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
base_folder=/home/install/won/int

# if the GENERATE_NEW_CERTIFICATES flag is set to true then setup things in a way that the certificates are recreated
# that (currently) includes:
# - deleting content of the server and client certificate folder
# - emptying the postgres database (all need data is lost!) => is done later in the script anyway
if [ "$GENERATE_NEW_CERTIFICATES" = true ] ; then
  echo generating new certificates! Old files and postgres need database will be deleted!
  ssh root@satsrv04 rm -rf $base_folder/won-server-certs
  ssh root@satsrv05 rm -rf $base_folder/won-server-certs
  ssh root@satsrv06 rm -rf $base_folder/won-server-certs
  ssh root@satsrv04 rm -rf $base_folder/won-client-certs
  ssh root@satsrv05 rm -rf $base_folder/won-client-certs
  ssh root@satsrv06 rm -rf $base_folder/won-client-certs
fi

# build the won docker images on every server of the cluster so that everywhere is the latest version available
echo start docker build of images:

ssh root@satsrv04 mkdir -p $base_folder/won-server-certs
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
ssh root@satsrv04 mkdir -p $base_folder/won-client-certs
ssh root@satsrv05 mkdir -p $base_folder/won-client-certs
ssh root@satsrv06 mkdir -p $base_folder/won-client-certs


# start the won containers on dedicated servers of the cluster
echo run docker containers:

# copy the openssl.conf file to the server where the certificates are generated
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
scp $WORKSPACE/webofneeds/won-docker/gencert/openssl-int.conf root@satsrv05:$base_folder/openssl-int.conf

# wonnode/owner server certificate generator
# Please note that value of PASS (if you set a non-default password) should be the same used in your server.xml for
# SSLPassword on wonnode and owner, and the same as activemq.broker.keystore.password used in your wonnode activemq
# spring configurations for broker, set the password with "-e PASS=pass:<your_password>" or "-e
# PASS=file:<your_file_with_password>"

# satsrv04 => standard certificate creation for this host only
docker -H satsrv04:2375 build -t webofneeds/gencert:int $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv04:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv04:2375 run --name=gencert_int -e CN="satsrv04.researchstudio.at" \
-e "PASS=pass:${won_certificate_passwd}" -v $base_folder/won-server-certs:/usr/local/certs/out/ webofneeds/gencert:int

# satsrv05 => certificate creation for multiple hosts
docker -H satsrv05:2375 build -t webofneeds/gencert:int $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv05:2375 rm gencert_int || echo 'No docker container found to remove with name: gencert_int'
docker -H satsrv05:2375 run --name=gencert_int -e CN="satsrv06.researchstudio.at" \
-e "OPENSSL_CONFIG_FILE=/usr/local/openssl.conf"  -e "PASS=pass:${won_certificate_passwd}" \
-v $base_folder/won-server-certs:/usr/local/certs/out/ -v $base_folder/openssl-int.conf:/usr/local/openssl.conf \
webofneeds/gencert:int

# get the certificates and create a password file (for the nginx) to read the certificate
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
mkdir -p ~/won-server-certs
rm ~/won-server-certs/*
echo ${won_certificate_passwd} > ~/won-server-certs/won_certificate_passwd_file
rsync root@satsrv05:$base_folder/won-server-certs/* ~/won-server-certs/
rsync ~/won-server-certs/* root@satsrv06:$base_folder/won-server-certs/

# copy the nginx.conf file to the proxy server
rsync $WORKSPACE/webofneeds/won-docker/nginx/nginx-int.conf root@satsrv06:$base_folder/nginx-int.conf

echo run nginx proxy server
if ! docker -H satsrv06:2375 run --name=nginx_int -v $base_folder/won-server-certs:/etc/nginx/won-server-certs/ \
-v $base_folder/nginx-int.conf:/etc/nginx/nginx.conf -d -p 80:80 -p 443:443 -p 61617:61617 nginx; then
  echo nginx container already available, restart old container
  docker -H satsrv06:2375 restart nginx_int
fi


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


# wonnode 1
docker -H satsrv04:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv04:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv04:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv04:2375 run --name=wonnode_int -d -e "uri.host=satsrv04.researchstudio.at" -e "http.port=8889" -e \
"activemq.broker.port=61617" -p 8889:8443 -p 61617:61617 -p 62911:62911 \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode_int:/usr/local/tomcat/won/client-certs/ \
-v $base_folder/agent:/opt/agent/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 9010:9010 \
-e "JMX_OPTS=-javaagent:/opt/agent/inspectit-agent.jar -Dinspectit.repository=satsrv07.researchstudio.at:9070
-Dinspectit.agent.name=wonnode_int_satsrv04
-Xdebug -Xrunjdwp:transport=dt_socket,address=62911,server=y,suspend=n
-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010
-Djava.rmi.server.hostname=satsrv04.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
webofneeds/wonnode:int

# wonnode 2 (used with the nginx proxy that runs on satsrv06)
docker -H satsrv05:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv05:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv05:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv05:2375 run --name=wonnode_int -d -e "uri.host=satsrv06.researchstudio.at" -e "http.port=8889" \
-e "uri.prefix=https://satsrv06.researchstudio.at/won" \
-e "client.authentication.behind.proxy=true" \
-e "activemq.broker.port=61617" -p 8889:8443 -p 61617:61617 -p 62911:62911 \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode_int:/usr/local/tomcat/won/client-certs/ \
-v $base_folder/agent:/opt/agent/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 9010:9010 \
-e "JMX_OPTS=-javaagent:/opt/agent/inspectit-agent.jar -Dinspectit.repository=satsrv07.researchstudio.at:9070
-Dinspectit.agent.name=wonnode_int_satsrv05
-Xdebug -Xrunjdwp:transport=dt_socket,address=62911,server=y,suspend=n
-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010
-Djava.rmi.server.hostname=satsrv05.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
webofneeds/wonnode:int

sleep 20

# bigdata
docker -H satsrv06:2375 pull webofneeds/bigdata
docker -H satsrv06:2375 stop bigdata_int || echo 'No docker container found to stop with name: bigdata_int'
docker -H satsrv06:2375 rm bigdata_int || echo 'No docker container found to remove with name: bigdata_int'
docker -H satsrv06:2375 run --name=bigdata_int -d -p 10000:9999 \
-m 256m webofneeds/bigdata

# matcher service
docker -H satsrv06:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv06:2375 stop matcher_service_int || echo 'No docker container found to stop with name: matcher_service_int'
docker -H satsrv06:2375 rm matcher_service_int || echo 'No docker container found to remove with name: matcher_service_int'
docker -H satsrv06:2375 run --name=matcher_service_int -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" \
-e "uri.sparql.endpoint=http://satsrv06.researchstudio.at:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://satsrv04.researchstudio.at:8889/won/resource,https://satsrv06.researchstudio.at/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -p 2561:2561 -p 62913:62913 \
-v $base_folder/won-client-certs/matcher_service_int:/usr/src/matcher-service/client-certs/ \
-p 9010:9010 \
-e "JMX_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=62913,server=y,suspend=n
-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9010
-Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
webofneeds/matcher_service:int

# solr server
docker -H satsrv06:2375 pull webofneeds/solr
docker -H satsrv06:2375 stop solr_int || echo 'No docker container found to stop with name: solr_int'
docker -H satsrv06:2375 rm solr_int || echo 'No docker container found to remove with name: solr_int'
docker -H satsrv06:2375 run --name=solr_int -d -p 7071:8080 -p 8984:8983 \
-p 9012:9012 \
-e CATALINA_OPTS="-Xmx200m  -XX:MaxPermSize=150m -XX:+HeapDumpOnOutOfMemoryError -Dcom.sun.management.jmxremote.port=9012 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9012 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-m 350m webofneeds/solr

# expect OWNER won-mail-sender host, user and password (i.e. configuration for no-replay won-owner-app-email-account) be
# set as environment variables, e.g. MAIL_USER=changeuser MAIL_PASS=changepass MAIL_HOST=smtp.changehost.com
echo ${MAIL_USER} at ${MAIL_HOST} is used as owner no-replay won-owner-app-email-account

# owner 1
docker -H satsrv04:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv04:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv04:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
docker -H satsrv04:2375 run --name=owner_int -d -e "node.default.host=satsrv04.researchstudio.at" \
-e "uri.host=satsrv04.researchstudio.at" -e "http.port=8082" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-e "node.default.http.port=8889" -p 8082:8443 -p 62912:62912 \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner_int:/usr/local/tomcat/won/client-certs/ \
-v $base_folder/agent:/opt/agent/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 9011:9011 \
-e "JMX_OPTS=-javaagent:/opt/agent/inspectit-agent.jar -Dinspectit.repository=satsrv07.researchstudio.at:9070
-Dinspectit.agent.name=owner_int_satsrv04
-Xdebug -Xrunjdwp:transport=dt_socket,address=62912,server=y,suspend=n
-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011
-Djava.rmi.server.hostname=satsrv04.researchstudio.at" \
-e "JMEM_OPTS=-Xmx1000m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/owner:int

# owner 2 (behind proxy on satsrv06)
docker -H satsrv05:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv05:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv05:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
docker -H satsrv05:2375 run --name=owner_int -d -e "node.default.host=satsrv06.researchstudio.at" \
-e "uri.host=satsrv06.researchstudio.at" -e "http.port=8082" \
-e "uri.prefix=https://satsrv06.researchstudio.at" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-e "node.default.http.port=443" -p 8082:8443 -p 62912:62912 \
-e "uri.prefix.node.default=https://satsrv06.researchstudio.at/won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner_int:/usr/local/tomcat/won/client-certs/ \
-v $base_folder/agent:/opt/agent/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 9011:9011 \
-e "JMX_OPTS=-javaagent:/opt/agent/inspectit-agent.jar -Dinspectit.repository=satsrv07.researchstudio.at:9070
-Dinspectit.agent.name=owner_int_satsrv05
-Xdebug -Xrunjdwp:transport=dt_socket,address=62912,server=y,suspend=n
-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011
-Djava.rmi.server.hostname=satsrv05.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
webofneeds/owner:int

sleep 10

# solr matcher
docker -H satsrv06:2375 build -t webofneeds/matcher_solr:int $WORKSPACE/webofneeds/won-docker/matcher-solr/
docker -H satsrv06:2375 stop matcher_solr_int || echo 'No docker container found to stop with name: matcher_solr_int'
docker -H satsrv06:2375 rm matcher_solr_int || echo 'No docker container found to remove with name: matcher_solr_int'
docker -H satsrv06:2375 run --name=matcher_solr_int -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-e "matcher.solr.uri.solr.server=http://satsrv06.researchstudio.at:8984/solr/" \
-e "matcher.solr.uri.solr.server.public=http://satsrv06.researchstudio.at:8984/solr/" \
-p 9011:9011 -p 62914:62914 \
-e "JMX_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=62914,server=y,suspend=n
-Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9011
-Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m \
-p 2562:2562 webofneeds/matcher_solr:int


sleep 20
docker -H satsrv06:2375 build -t webofneeds/bots:int $WORKSPACE/webofneeds/won-docker/bots/
docker -H satsrv06:2375 stop need_creator_bot_int || echo 'No docker container found to stop with name: need_creator_bot_int'
docker -H satsrv06:2375 rm need_creator_bot_int || echo 'No docker container found to remove with name: need_creator_bot_int'
docker -H satsrv06:2375 stop echo_bot_int || echo 'No docker container found to stop with name: echo_bot_int'
docker -H satsrv06:2375 rm echo_bot_int || echo 'No docker container found to remove with name: echo_bot_int'
docker -H satsrv06:2375 stop debug_bot_int || echo 'No docker container found to stop with name: debug_bot_int'
docker -H satsrv06:2375 rm debug_bot_int || echo 'No docker container found to remove with name: debug_bot_int'
docker -H satsrv06:2375 run --name=debug_bot_int -d \
-e "node.default.host=satsrv04.researchstudio.at" -e "node.default.http.port=8889" \
-e "won.node.uris=https://satsrv04.researchstudio.at:8889/won/resource https://satsrv06.researchstudio.at/won/resource" \
-p 9013:9013 \
-e "JMX_OPTS=-Dcom.sun.management.jmxremote.port=9013 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.rmi.port=9013 -Djava.rmi.server.hostname=satsrv06.researchstudio.at" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 300m \
webofneeds/bots:int
