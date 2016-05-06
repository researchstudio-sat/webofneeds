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
# - start siren solr server on satsrv06 as a need index
# - start siren matcher on satsrv06 as a matcher and connect to matcher service
##############################################################################################################


# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
base_folder=/home/install/won/dev
jenkins_base_folder=/var/lib/jenkins/won/dev
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
  docker -H satsrv04:2375 stop postgres_dev || echo 'No docker container found to stop with name: postgres_dev'
  docker -H satsrv05:2375 stop postgres_dev || echo 'No docker container found to stop with name: postgres_dev'
  docker -H satsrv04:2375 rm postgres_dev || echo 'No docker container found to remove with name: postgres_dev'
  docker -H satsrv05:2375 rm postgres_dev || echo 'No docker container found to remove with name: postgres_dev'
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
scp $WORKSPACE/webofneeds/won-docker/gencert/openssl-dev.conf root@satsrv05:$base_folder/openssl-dev.conf

# wonnode/owner server certificate generator
# Please note that value of PASS (if you set a non-default password) should be the same used in your server.xml for
# SSLPassword on wonnode and owner, and the same as activemq.broker.keystore.password used in your wonnode activemq
# spring configurations for broker, set the password with "-e PASS=pass:<your_password>" or "-e
# PASS=file:<your_file_with_password>"

# satsrv04 => standard certificate creation for this host only
docker -H satsrv04:2375 build -t webofneeds/gencert:dev $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv04:2375 rm gencert_dev || echo 'No docker container found to remove with name: gencert_dev'
docker -H satsrv04:2375 run --name=gencert_dev -e CN="satsrv04.researchstudio.at" \
-e "PASS=pass:${won_certificate_passwd}" -v $base_folder/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:dev

# satsrv05 => certificate creation for multiple hosts
docker -H satsrv05:2375 build -t webofneeds/gencert:dev $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satsrv05:2375 rm gencert_dev || echo 'No docker container found to remove with name: gencert_dev'
docker -H satsrv05:2375 run --name=gencert_dev -e CN="satsrv07.researchstudio.at" \
-e "OPENSSL_CONFIG_FILE=/usr/local/openssl.conf" -e "PASS=pass:${won_certificate_passwd}" \
-v $base_folder/won-server-certs:/usr/local/certs/out/ -v $base_folder/openssl-dev.conf:/usr/local/openssl.conf \
webofneeds/gencert:dev

# get the certificates and create a password file (for the nginx) to read the certificate
echo ${won_certificate_passwd} > $jenkins_base_folder/won-server-certs/won_certificate_passwd_file
rsync root@satsrv05:$base_folder/won-server-certs/* $jenkins_base_folder/won-server-certs/

echo run nginx proxy server
if ! docker -H satsrv07:2375 run --name=nginx_dev -v $jenkins_base_folder/won-server-certs:/etc/nginx/won-server-certs/ \
-v $WORKSPACE/webofneeds/won-docker/nginx/nginx-dev.conf:/etc/nginx/nginx.conf -d -p 80:80 -p 443:443 -p 61616:61616 nginx; then
  echo nginx container already available, restart old container
  docker -H satsrv07:2375 restart nginx_dev
fi


# NOTE: do not redeploy the postgres database for won node and owner to keep the data after deployments
# postgres db 1
docker -H satsrv04:2375 pull webofneeds/postgres
#docker -H satsrv04:2375 stop postgres_dev  || echo 'No docker container found to stop with name: postgres_dev'
#docker -H satsrv04:2375 rm postgres_dev || echo 'No docker container found to remove with name: postgres_dev'
echo try to start new postgres container
if ! docker -H satsrv04:2375 run --name=postgres_dev -d -p 5432:5432 -m 256m webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker -H satsrv04:2375 restart postgres_dev
fi


# postgres db 2
docker -H satsrv05:2375 pull webofneeds/postgres
#docker -H satsrv05:2375 stop postgres_dev || echo 'No docker container found to stop with name: postgres_dev'
#docker -H satsrv05:2375 rm postgres_dev || echo 'No docker container found to remove with name: postgres_dev'
echo try to start new postgres container
if ! docker -H satsrv05:2375 run --name=postgres_dev -d -p 5432:5432 -m 256m webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker -H satsrv05:2375 restart postgres_dev
fi

sleep 10

# wonnode 1
docker -H satsrv04:2375 build -t webofneeds/wonnode:dev $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv04:2375 stop wonnode_dev || echo 'No docker container found to stop with name: wonnode_dev'
docker -H satsrv04:2375 rm wonnode_dev || echo 'No docker container found to remove with name: wonnode_dev'
docker -H satsrv04:2375 run --name=wonnode_dev -d -e "uri.host=satsrv04.researchstudio.at" \
-e "http.port=8888" -e "activemq.broker.port=61616" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5432/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode_dev:/usr/local/tomcat/won/client-certs/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 8888:8443 -p 61616:61616 \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m webofneeds/wonnode:dev


# wonnode 2 (used with the nginx proxy that runs on satsrv07)
docker -H satsrv05:2375 build -t webofneeds/wonnode:dev $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv05:2375 stop wonnode_dev || echo 'No docker container found to stop with name: wonnode_dev'
docker -H satsrv05:2375 rm wonnode_dev || echo 'No docker container found to remove with name: wonnode_dev'
docker -H satsrv05:2375 run --name=wonnode_dev -d -e "uri.host=satsrv07.researchstudio.at" \
-e "http.port=8888" -e "activemq.broker.port=61616" \
-e "uri.prefix=https://satsrv07.researchstudio.at/won" \
-e "client.authentication.behind.proxy=true" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5432/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode_dev:/usr/local/tomcat/won/client-certs/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 8888:8443 -p 61616:61616 \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m webofneeds/wonnode:dev

sleep 20

# bigdata
docker -H satsrv06:2375 pull webofneeds/bigdata
docker -H satsrv06:2375 stop bigdata_dev || echo 'No docker container found to stop with name: bigdata_dev'
docker -H satsrv06:2375 rm bigdata_dev || echo 'No docker container found to remove with name: bigdata_dev'
docker -H satsrv06:2375 run --name=bigdata_dev -d -p 9999:9999 \
-m 256m webofneeds/bigdata

# matcher service
docker -H satsrv06:2375 build -t webofneeds/matcher_service:dev $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv06:2375 stop matcher_service_dev || echo 'No docker container found to stop with name: matcher_service_dev'
docker -H satsrv06:2375 rm matcher_service_dev || echo 'No docker container found to remove with name: matcher_service_dev'
docker -H satsrv06:2375 run --name=matcher_service_dev -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" \
-e "uri.sparql.endpoint=http://satsrv06.researchstudio.at:9999/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://satsrv04.researchstudio.at:8888/won/resource,https://satsrv07.researchstudio.at/won/resource" \
-v $base_folder/won-client-certs/matcher_service_dev:/usr/src/matcher-service/client-certs/ \
-e "cluster.local.port=2551" -e "cluster.seed.port=2551" -p 2551:2551 \
-e "JMEM_OPTS=-Xmx170m -XX:MaxMetaspaceSize=160m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m webofneeds/matcher_service:dev

# siren solr server
docker -H satsrv06:2375 pull webofneeds/sirensolr
docker -H satsrv06:2375 stop sirensolr_dev || echo 'No docker container found to stop with name: sirensolr_dev'
docker -H satsrv06:2375 rm sirensolr_dev || echo 'No docker container found to remove with name: sirensolr_dev'
docker -H satsrv06:2375 run --name=sirensolr_dev -d -p 7070:8080 -p 8983:8983 --env CATALINA_OPTS="-Xmx200m  -XX:MaxPermSize=150m -XX:+HeapDumpOnOutOfMemoryError" -m 350m webofneeds/sirensolr


# expect OWNER won-mail-sender host, user and password (i.e. configuration for no-replay won-owner-app-email-account) be
# set as environment variables, e.g. MAIL_USER=changeuser MAIL_PASS=changepass MAIL_HOST=smtp.changehost.com
echo ${MAIL_USER} at ${MAIL_HOST} is used as owner no-replay won-owner-app-email-account

# owner 1
docker -H satsrv04:2375 build -t webofneeds/owner:dev $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv04:2375 stop owner_dev || echo 'No docker container found to stop with name: owner_dev'
docker -H satsrv04:2375 rm owner_dev || echo 'No docker container found to remove with name: owner_dev'
docker -H satsrv04:2375 run --name=owner_dev -d -e "node.default.host=satsrv04.researchstudio.at" \
-e "node.default.http.port=8888" \
-e "uri.host=satsrv04.researchstudio.at" -e "http.port=8081" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5432/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner_dev:/usr/local/tomcat/won/client-certs/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 8081:8443 \
-e "JMEM_OPTS=-Xmx1000m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/owner:dev

# owner 2
docker -H satsrv05:2375 build -t webofneeds/owner:dev $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv05:2375 stop owner_dev || echo 'No docker container found to stop with name: owner_dev'
docker -H satsrv05:2375 rm owner_dev || echo 'No docker container found to remove with name: owner_dev'
docker -H satsrv05:2375 run --name=owner_dev -d -e "node.default.host=satsrv07.researchstudio.at" \
-e "node.default.http.port=443" -e "uri.host=satsrv07.researchstudio.at" -e "http.port=8081" \
-e "uri.prefix.node.default=https://satsrv07.researchstudio.at/won" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5432/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner_dev:/usr/local/tomcat/won/client-certs/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-p 8081:8443 \
-e "JMEM_OPTS=-Xmx1000m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/owner:dev

sleep 10

# siren matcher
docker -H satsrv06:2375 build -t webofneeds/matcher_siren:dev $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv06:2375 stop matcher_siren_dev || echo 'No docker container found to stop with name: matcher_siren_dev'
docker -H satsrv06:2375 rm matcher_siren_dev || echo 'No docker container found to remove with name: matcher_siren_dev'
docker -H satsrv06:2375 run --name=matcher_siren_dev -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" -e "cluster.seed.port=2551" -e "cluster.local.port=2552" \
-e "matcher.siren.uri.solr.server=http://satsrv06.researchstudio.at:8983/solr/won/" \
-e "matcher.siren.uri.solr.server.public=http://satsrv06.researchstudio.at:8983/solr/#/won/" \
-p 2552:2552 \
-e "JMEM_OPTS=-Xmx200m -XX:MaxMetaspaceSize=150m -XX:+HeapDumpOnOutOfMemoryError" \
-m 350m webofneeds/matcher_siren:dev



# if everything works up to this point - build :dev images locally and push these local images into the dockerhub:
# build:
docker -H localhost:2375 build -t webofneeds/gencert:dev $WORKSPACE/webofneeds/won-docker/gencert/
docker -H localhost:2375 build -t webofneeds/wonnode:dev $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H localhost:2375 build -t webofneeds/owner:dev $WORKSPACE/webofneeds/won-docker/owner/
docker -H localhost:2375 build -t webofneeds/matcher_service:dev $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H localhost:2375 build -t webofneeds/matcher_siren:dev $WORKSPACE/webofneeds/won-docker/matcher-siren/
# push:
docker -H localhost:2375 login -u heikofriedrich
docker -H localhost:2375 push webofneeds/gencert:dev
docker -H localhost:2375 push webofneeds/wonnode:dev
docker -H localhost:2375 push webofneeds/owner:dev
docker -H localhost:2375 push webofneeds/matcher_service:dev
docker -H localhost:2375 push webofneeds/matcher_siren:dev
