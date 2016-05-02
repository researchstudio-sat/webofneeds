# fail the whole script if one command fails
set -e

# ====================================
# set deployment specific variables:
# ====================================

# full hostname where application is deployed
deploy_host=satcluster01.researchstudio.at

# docker options to execute docker commands with
# docker_options="-H satcluster01.researchstudio.at:2375"
if [ -z "$docker_options" ]; then
  docker_options=
fi

# set this to true if using a reverse proxy server that takes care of client certificate authentication
# behind_proxy=true
if [ -z "$behind_proxy" ]; then
  behind_proxy=false
fi

# public node uri, used in the linked data uris, default if not set use deploy_host
# public_node_uri="www.matchat.org"
if [ -z "$public_node_uri" ]; then
  public_node_uri=${deploy_host}
fi

# base folder is used to mount some files (e.g. certificates) from the server into the containers
# base_folder=//c//Users//<path> (Windows)
base_folder=/home/install

# set password for the server certificate, if it is not already set
if [ -z "$won_certificate_passwd" ]; then
  won_certificate_passwd=changeit
fi

# configure the mail server, if needed
# MAIL_USER=
# MAIL_PASS=
# MAIL_HOST=

# ====================================


# start the won containers on dedicated servers of the cluster
echo run docker containers:

# a new certificate is created in the base_folder/won-server-certs dir if there is not already one available
echo create certificate if not available
docker ${docker_options} rm gencert_ma || echo 'No docker container found to remove with name: gencert_ma'
docker ${docker_options} run --name=gencert_ma -e CN="${deploy_host}" \
-e "PASS=pass:${won_certificate_passwd}" -v $base_folder/won-server-certs:/usr/local/certs/out/ \
webofneeds/gencert:master

# postgres
# NOTE: do not redeploy the postgres database for won node and owner to keep the data after deployments postgres db
echo run postgres container
docker ${docker_options} pull webofneeds/postgres
if ! docker ${docker_options} run --name=postgres_ma -d -p 5433:5432 webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker ${docker_options} restart postgres_ma
fi

sleep 10

# wonnode
echo run wonnode container
docker ${docker_options} stop wonnode_ma || echo 'No docker container found to stop with name: wonnode_ma'
docker ${docker_options} rm wonnode_ma || echo 'No docker container found to remove with name: wonnode_ma'
docker ${docker_options} run --name=wonnode_ma -d -e "uri.host=$public_node_uri" -e "http.port=443" \
-e "uri.prefix=https://${public_node_uri}/won" \
-e "activemq.broker.port=61617" -p 443:8443 -p 61617:61617 \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode_ma:/usr/local/tomcat/won/client-certs/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-e "client.authentication.behind.proxy=$behind_proxy" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://${deploy_host}:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "JMEM_OPTS=-Xmx400m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/wonnode:master

# bigdata
echo run bigdata container
docker ${docker_options} pull webofneeds/bigdata
docker ${docker_options} stop bigdata_ma || echo 'No docker container found to stop with name: bigdata_ma'
docker ${docker_options} rm bigdata_ma || echo 'No docker container found to remove with name: bigdata_ma'
docker ${docker_options} run --name=bigdata_ma -d -p 10000:9999 -m 400m webofneeds/bigdata

sleep 10

# matcher service
echo run matcher service container
docker ${docker_options} stop matcher_service_ma || echo 'No docker container found to stop with name: matcher_service_ma'
docker ${docker_options} rm matcher_service_ma || echo 'No docker container found to remove with name: matcher_service_ma'
docker ${docker_options} run --name=matcher_service_ma -d -e "node.host=${deploy_host}" \
-e "cluster.seed.host=${deploy_host}" \
-e "uri.sparql.endpoint=http://${deploy_host}:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://${public_node_uri}/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -p 2561:2561 \
-v $base_folder/won-client-certs/matcher_service_ma:/usr/src/matcher-service/client-certs/ \
-e "JMEM_OPTS=-Xmx250m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/matcher_service:master

# siren solr server
echo run solr server container
docker ${docker_options} pull webofneeds/sirensolr
docker ${docker_options} stop sirensolr_ma || echo 'No docker container found to stop with name: sirensolr_ma'
docker ${docker_options} rm sirensolr_ma || echo 'No docker container found to remove with name: sirensolr_ma'
docker ${docker_options} run --name=sirensolr_ma -d -p 7071:8080 -p 8984:8983 --env CATALINA_OPTS="-Xmx200m \
-XX:MaxPermSize=150m -XX:+HeapDumpOnOutOfMemoryError" webofneeds/sirensolr

sleep 10

# expect OWNER won-mail-sender host, user and password (i.e. configuration for no-replay won-owner-app-email-account) be
# set as environment variables, e.g. MAIL_USER=changeuser MAIL_PASS=changepass MAIL_HOST=smtp.changehost.com
echo ${MAIL_USER} at ${MAIL_HOST} is used as owner no-replay won-owner-app-email-account

# owner
echo run owner container
docker ${docker_options} stop owner_ma || echo 'No docker container found to stop with name: owner_ma'
docker ${docker_options} rm owner_ma || echo 'No docker container found to remove with name: owner_ma'
docker ${docker_options} run --name=owner_ma -d -e "node.default.host=$public_node_uri" \
-e "node.default.http.port=443" -p 8082:8443 \
-e "uri.host=$public_node_uri" -e "http.port=8082" \
-e "uri.prefix.node.default=https://${public_node_uri}/won" \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner_ma:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://${deploy_host}:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
webofneeds/owner:master

# siren matcher
echo run siren matcher container
docker ${docker_options} stop matcher_siren_ma || echo 'No docker container found to stop with name: matcher_siren_ma'
docker ${docker_options} rm matcher_siren_ma || echo 'No docker container found to remove with name: matcher_siren_ma'
docker ${docker_options} run --name=matcher_siren_ma -d -e "node.host=${deploy_host}" \
-e "cluster.seed.host=${deploy_host}" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-e "matcher.siren.uri.solr.server=http://${deploy_host}:8984/solr/won/" \
-e "matcher.siren.uri.solr.server.public=http://${deploy_host}:8984/solr/#/won/" \
-p 2562:2562 -e "JMEM_OPTS=-Xmx250m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/matcher_siren:master
