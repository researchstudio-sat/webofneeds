#############################################################################################################
# Deployment descriptions:
# This script deploys several docker containers of the won application of the following servers:
#
# - create server certificate for satsrv04
# - create server certificate for satsrv05 and satsrv07
# - start nginx proxy server on satsrv07 for owner and wonnode on host satsrv05
# - start portsgres need databases for owner and wonnode on satsrv04 and satsrv05
# - start wonnode on satsrv04 (without proxy) => https://satsrv04:8888/won
# - start wonnode on satsrv05 (with proxy on satsrv07) => https://satsrv07/won
# - start owner on satsrv04 (without proxy) => https://satsrv04:8081/owner
# - start owner on satsrv05 (with proxy on satsrv07) => https://satsrv07/
# - start bigdata rdf store on satsrv06 for matcher service
# - start matcher service on satsrv06 and connect with wonnodes on satsrv04 and proxied wonnode on satsrv05
# - start solr server on satsrv06 as a need index
# - start solr matcher on satsrv06 as a matcher and connect to matcher service
#
# The databases (postgres), rdf-stores (bigdata) and indices (solr) are kept between deployments and are only
# deleted and created new if the certificate changes and the postgres db has to be recreated.
##############################################################################################################


# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
base_folder=/home/install/won/master
jenkins_base_folder=/var/lib/jenkins/won/master
mkdir -p $jenkins_base_folder

# if the GENERATE_NEW_CERTIFICATES flag is set to true then setup things in a way that the certificates are recreated
# that (currently) includes:
# - deleting content of the server and client certificate folder
# - emptying the postgres database (all need data is lost!)
if [ "$GENERATE_NEW_CERTIFICATES" = true ] ; then
  echo generating new certificates! Old files and postgres need database will be deleted!
  ssh root@satsrv04 rm -rf $base_folder/won-server-certs
  ssh root@satsrv05 rm -rf $base_folder/won-server-certs
  ssh root@satsrv06 rm -rf $base_folder/won-server-certs
  ssh root@satsrv04 rm -rf $base_folder/won-client-certs
  ssh root@satsrv05 rm -rf $base_folder/won-client-certs
  ssh root@satsrv06 rm -rf $base_folder/won-client-certs
  rm -rf $jenkins_base_folder/won-server-certs

  docker -H satsrv04:2375 stop postgres_master || echo 'No docker container found to stop with name: postgres_master'
  docker -H satsrv05:2375 stop postgres_master || echo 'No docker container found to stop with name: postgres_master'
  docker -H satsrv04:2375 rm postgres_master || echo 'No docker container found to remove with name: postgres_master'
  docker -H satsrv05:2375 rm postgres_master || echo 'No docker container found to remove with name: postgres_master'
  docker -H satsrv06:2375 stop bigdata_master || echo 'No docker container found to stop with name: bigdata_master'
  docker -H satsrv06:2375 rm bigdata_master || echo 'No docker container found to remove with name: bigdata_master'
  docker -H satsrv06:2375 stop solr_master || echo 'No docker container found to stop with name: solr_master'
  docker -H satsrv06:2375 rm solr_master || echo 'No docker container found to remove with name: solr_master'
fi

# build won docker images and deploy to sat cluster
echo start docker build and deployment:

ssh root@satsrv04 mkdir -p $base_folder/won-server-certs
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
ssh root@satsrv04 mkdir -p $base_folder/won-client-certs
ssh root@satsrv05 mkdir -p $base_folder/won-client-certs
ssh root@satsrv06 mkdir -p $base_folder/won-client-certs
mkdir -p $jenkins_base_folder/won-server-certs


# copy the openssl.conf file to the server where the certificates are generated
scp $WORKSPACE/webofneeds/won-docker/gencert/openssl-master.conf root@satsrv05:$base_folder/openssl-master.conf

# wonnode/owner server certificate generator
# Please note that value of PASS (if you set a non-default password) should be the same used in your server.xml for
# SSLPassword on wonnode and owner, and the same as activemq.broker.keystore.password used in your wonnode activemq
# spring configurations for broker, set the password with "-e PASS=pass:<your_password>" or "-e
# PASS=file:<your_file_with_password>"

# satsrv04 => standard certificate creation for this host only
docker -H satsrv04:2375 build -t webofneeds/gencert:maa $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv04:2375 rm gencert_master || echo 'No docker container found to remove with name: gencert_master'
docker -H satsrv04:2375 run --name=gencert_master -e CN="satsrv04.researchstudio.at" \
-e "PASS=pass:${won_certificate_passwd}" -v $base_folder/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:master

# satsrv05 => certificate creation for multiple hosts
docker -H satsrv05:2375 build -t webofneeds/gencert:master $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv05:2375 rm gencert_master || echo 'No docker container found to remove with name: gencert_master'
docker -H satsrv05:2375 run --name=gencert_master -e CN="satsrv07.researchstudio.at" \
-e "OPENSSL_CONFIG_FILE=/usr/local/openssl.conf" -e "PASS=pass:${won_certificate_passwd}" \
-v $base_folder/won-server-certs:/usr/local/certs/out/ -v $base_folder/openssl-master.conf:/usr/local/openssl.conf \
webofneeds/gencert:master

# get the certificates and create a password file (for the nginx) to read the certificate
echo ${won_certificate_passwd} > $jenkins_base_folder/won-server-certs/won_certificate_passwd_file
rsync root@satsrv05:$base_folder/won-server-certs/* $jenkins_base_folder/won-server-certs/

echo run nginx proxy server
if ! docker -H satsrv07:2375 run --name=nginx_master -v $jenkins_base_folder/won-server-certs:/etc/nginx/won-server-certs/ \
-v $WORKSPACE/webofneeds/won-docker/nginx/nginx-master.conf:/etc/nginx/nginx.conf -d -p 80:80 -p 443:443 -p 61616:61616 nginx; then
  echo nginx container already available, restart old container
  docker -H satsrv07:2375 restart nginx_master
fi


# NOTE: do not redeploy the postgres database for won node and owner to keep the data after deployments
# postgres db 1
docker -H satsrv04:2375 pull webofneeds/postgres
#docker -H satsrv04:2375 stop postgres_master  || echo 'No docker container found to stop with name: postgres_master'
#docker -H satsrv04:2375 rm postgres_master || echo 'No docker container found to remove with name: postgres_master'
echo try to start new postgres container
if ! docker -H satsrv04:2375 run --name=postgres_master -d -p 5432:5432 -m 256m webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker -H satsrv04:2375 restart postgres_master
fi


# postgres db 2
docker -H satsrv05:2375 pull webofneeds/postgres
#docker -H satsrv05:2375 stop postgres_master || echo 'No docker container found to stop with name: postgres_master'
#docker -H satsrv05:2375 rm postgres_master || echo 'No docker container found to remove with name: postgres_master'
echo try to start new postgres container
if ! docker -H satsrv05:2375 run --name=postgres_master -d -p 5432:5432 -m 256m webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker -H satsrv05:2375 restart postgres_master
fi

sleep 10

# wonnode 1
docker -H satsrv04:2375 build -t webofneeds/wonnode:master $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv04:2375 stop wonnode_master || echo 'No docker container found to stop with name: wonnode_master'
docker -H satsrv04:2375 rm wonnode_master || echo 'No docker container found to remove with name: wonnode_master'
docker -H satsrv04:2375 run --name=wonnode_master -d -e "uri.host=satsrv04.researchstudio.at" \
-e "http.port=8888" -e "activemq.broker.port=61616" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5432/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode_master:/usr/local/tomcat/won/client-certs/ \
-v $base_folder/agent:/opt/agent/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 8888:8443 -p 61616:61616 \
-e "JMX_OPTS=-javaagent:/opt/agent/inspectit-agent.jar -Dinspectit.repository=satsrv07.researchstudio.at:9070
-Dinspectit.agent.name=wonnode_master_satsrv04" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m webofneeds/wonnode:master


# wonnode 2 (used with the nginx proxy that runs on satsrv07)
docker -H satsrv05:2375 build -t webofneeds/wonnode:master $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv05:2375 stop wonnode_master || echo 'No docker container found to stop with name: wonnode_master'
docker -H satsrv05:2375 rm wonnode_master || echo 'No docker container found to remove with name: wonnode_master'
docker -H satsrv05:2375 run --name=wonnode_master -d -e "uri.host=satsrv07.researchstudio.at" \
-e "http.port=8888" -e "activemq.broker.port=61616" \
-e "uri.prefix=https://satsrv07.researchstudio.at/won" \
-e "client.authentication.behind.proxy=true" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5432/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode_master:/usr/local/tomcat/won/client-certs/ \
-v $base_folder/agent:/opt/agent/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 8888:8443 -p 61616:61616 \
-e "JMX_OPTS=-javaagent:/opt/agent/inspectit-agent.jar -Dinspectit.repository=satsrv07.researchstudio.at:9070
-Dinspectit.agent.name=wonnode_master_satsrv05" \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m webofneeds/wonnode:master

sleep 20

# bigdata
docker -H satsrv06:2375 pull webofneeds/bigdata
echo try to start new bigdata container
if ! docker -H satsrv06:2375 run --name=bigdata_master -d -p 9999:9999 -m 256m webofneeds/bigdata; then
  echo bigdata container already available, restart old container
  docker -H satsrv06:2375 restart bigdata_master
fi

# matcher service
docker -H satsrv06:2375 build -t webofneeds/matcher_service:master $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv06:2375 stop matcher_service_master || echo 'No docker container found to stop with name: matcher_service_master'
docker -H satsrv06:2375 rm matcher_service_master || echo 'No docker container found to remove with name: matcher_service_master'
docker -H satsrv06:2375 run --name=matcher_service_master -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" \
-e "uri.sparql.endpoint=http://satsrv06.researchstudio.at:9999/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://satsrv04.researchstudio.at:8888/won/resource,https://satsrv07.researchstudio.at/won/resource" \
-v $base_folder/won-client-certs/matcher_service_master:/usr/src/matcher-service/client-certs/ \
-e "cluster.local.port=2551" -e "cluster.seed.port=2551" -p 2551:2551 \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m webofneeds/matcher_service:master

# solr server
docker -H satsrv06:2375 pull webofneeds/solr
echo try to start new solr server container
if ! docker -H satsrv06:2375 run --name=solr_master -d -p 7070:8080 -p 8983:8983 \
--env CATALINA_OPTS="-Xmx200m  -XX:MaxPermSize=150m -XX:+HeapDumpOnOutOfMemoryError" -m 350m webofneeds/solr; then
  echo solr server container already available, restart old container
  docker -H satsrv06:2375 restart solr_master
fi


# expect OWNER won-mail-sender host, user and password (i.e. configuration for no-replay won-owner-app-email-account) be
# set as environment variables, e.g. MAIL_USER=changeuser MAIL_PASS=changepass MAIL_HOST=smtp.changehost.com
echo ${MAIL_USER} at ${MAIL_HOST} is used as owner no-replay won-owner-app-email-account

# owner 1
docker -H satsrv04:2375 build -t webofneeds/owner:master $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv04:2375 stop owner_master || echo 'No docker container found to stop with name: owner_master'
docker -H satsrv04:2375 rm owner_master || echo 'No docker container found to remove with name: owner_master'
docker -H satsrv04:2375 run --name=owner_master -d -e "node.default.host=satsrv04.researchstudio.at" \
-e "node.default.http.port=8888" \
-e "uri.host=satsrv04.researchstudio.at" -e "http.port=8081" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5432/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner_master:/usr/local/tomcat/won/client-certs/ \
-v $base_folder/agent:/opt/agent/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 8081:8443 \
-e "JMX_OPTS=-javaagent:/opt/agent/inspectit-agent.jar -Dinspectit.repository=satsrv07.researchstudio.at:9070
-Dinspectit.agent.name=owner_master_satsrv04" \
-e "JMEM_OPTS=-Xmx1000m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/owner:master

# owner 2 (behind proxy on satsrv07)
docker -H satsrv05:2375 build -t webofneeds/owner:master $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv05:2375 stop owner_master || echo 'No docker container found to stop with name: owner_master'
docker -H satsrv05:2375 rm owner_master || echo 'No docker container found to remove with name: owner_master'
docker -H satsrv05:2375 run --name=owner_master -d -e "node.default.host=satsrv07.researchstudio.at" \
-e "node.default.http.port=443" -e "uri.host=satsrv07.researchstudio.at" -e "http.port=8081" \
-e "uri.prefix=https://satsrv07.researchstudio.at" \
-e "uri.prefix.node.default=https://satsrv07.researchstudio.at/won" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5432/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner_master:/usr/local/tomcat/won/client-certs/ \
-v $base_folder/agent:/opt/agent/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 8081:8443 \
-e "JMX_OPTS=-javaagent:/opt/agent/inspectit-agent.jar -Dinspectit.repository=satsrv07.researchstudio.at:9070
-Dinspectit.agent.name=owner_master_satsrv05" \
-e "JMEM_OPTS=-Xmx1000m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/owner:master

sleep 10

# solr matcher
docker -H satsrv06:2375 build -t webofneeds/matcher_solr:master $WORKSPACE/webofneeds/won-docker/matcher-solr/
docker -H satsrv06:2375 stop matcher_solr_master || echo 'No docker container found to stop with name: matcher_solr_master'
docker -H satsrv06:2375 rm matcher_solr_master || echo 'No docker container found to remove with name: matcher_solr_master'
docker -H satsrv06:2375 run --name=matcher_solr_master -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" -e "cluster.seed.port=2551" -e "cluster.local.port=2552" \
-e "matcher.solr.uri.solr.server=http://satsrv06.researchstudio.at:8983/solr/" \
-e "matcher.solr.uri.solr.server.public=http://satsrv06.researchstudio.at:8983/solr/" \
-p 2552:2552 \
-e "JMEM_OPTS=-Xmx200m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m webofneeds/matcher_solr:master



# if everything works up to this point - build :master images locally and push these local images into the dockerhub:
# build:
docker -H localhost:2375 build -t webofneeds/gencert:master $WORKSPACE/webofneeds/won-docker/gencert/
docker -H localhost:2375 build -t webofneeds/wonnode:master $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H localhost:2375 build -t webofneeds/owner:master $WORKSPACE/webofneeds/won-docker/owner/
docker -H localhost:2375 build -t webofneeds/matcher_service:master $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H localhost:2375 build -t webofneeds/matcher_solr:master $WORKSPACE/webofneeds/won-docker/matcher-solr/
# push:
docker -H localhost:2375 login -u heikofriedrich
docker -H localhost:2375 push webofneeds/gencert:master
docker -H localhost:2375 push webofneeds/wonnode:master
docker -H localhost:2375 push webofneeds/owner:master
docker -H localhost:2375 push webofneeds/matcher_service:master
docker -H localhost:2375 push webofneeds/matcher_solr:master
