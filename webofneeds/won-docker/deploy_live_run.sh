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

# tag name of images to use for deployment (default uses master images)
if [ -z "$deploy_image_tag_name" ]; then
  deploy_image_tag_name=master
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

echo "deployment options: "
echo "docker_options: $docker_options"
echo "deploy_image_tag_name: $deploy_image_tag_name"
echo "public_node_uri: $public_node_uri"
echo "deploy_host: $deploy_host"
echo "behind_proxy: $behind_proxy"
echo "base_folder: $base_folder"

# start the won containers on dedicated servers of the cluster
echo run docker containers:

# a new certificate is created in the base_folder/won-server-certs dir if there is not already one available
echo create certificate if not available
docker ${docker_options} rm gencert || echo 'No docker container found to remove with name: gencert'
docker ${docker_options} run --name=gencert -e CN="${deploy_host}" \
-e "PASS=pass:${won_certificate_passwd}" -v $base_folder/won-server-certs:/usr/local/certs/out/ \
webofneeds/gencert:${deploy_image_tag_name}

# postgres
# NOTE: do not redeploy the postgres database for won node and owner to keep the data after deployments postgres db
echo run postgres container
docker ${docker_options} pull webofneeds/postgres
if ! docker ${docker_options} run --name=postgres -d -p 5433:5432 webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker ${docker_options} restart postgres
fi

sleep 10

# wonnode
echo run wonnode container
docker ${docker_options} stop wonnode || echo 'No docker container found to stop with name: wonnode'
docker ${docker_options} rm wonnode || echo 'No docker container found to remove with name: wonnode'
docker ${docker_options} run --name=wonnode -d -e "uri.host=$public_node_uri" -e "http.port=443" \
-e "uri.prefix=https://${public_node_uri}/won" \
-e "activemq.broker.port=61617" -p 443:8443 -p 61617:61617 \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode:/usr/local/tomcat/won/client-certs/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-e "client.authentication.behind.proxy=$behind_proxy" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://${deploy_host}:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "JMEM_OPTS=-Xmx400m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/wonnode:${deploy_image_tag_name}

# bigdata
echo run bigdata container
docker ${docker_options} pull webofneeds/bigdata
if ! docker ${docker_options} run --name=bigdata -d -p 10000:9999 -m 400m webofneeds/bigdata; then
  echo bigdata container already available, restart old container
  docker ${docker_options} restart bigdata
fi

sleep 10

# matcher service
echo run matcher service container
docker ${docker_options} stop matcher_service || echo 'No docker container found to stop with name: matcher_service'
docker ${docker_options} rm matcher_service || echo 'No docker container found to remove with name: matcher_service'
docker ${docker_options} run --name=matcher_service -d -e "node.host=${deploy_host}" \
-e "cluster.seed.host=${deploy_host}" \
-e "uri.sparql.endpoint=http://${deploy_host}:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://${public_node_uri}/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -p 2561:2561 \
-v $base_folder/won-client-certs/matcher_service:/usr/src/matcher-service/client-certs/ \
-e "JMEM_OPTS=-Xmx250m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/matcher_service:${deploy_image_tag_name}

# solr server
echo run solr server container
docker ${docker_options} pull webofneeds/solr
if ! docker ${docker_options} run --name=solr -d -p 7071:8080 -p 8984:8983 --env CATALINA_OPTS="-Xmx200m \
     -XX:MaxPermSize=150m -XX:+HeapDumpOnOutOfMemoryError" webofneeds/solr; then
  echo solr server container already available, restart old container
  docker ${docker_options} restart solr
fi



sleep 10

# expect OWNER won-mail-sender host, user and password (i.e. configuration for no-replay won-owner-app-email-account) be
# set as environment variables, e.g. MAIL_USER=changeuser MAIL_PASS=changepass MAIL_HOST=smtp.changehost.com
echo ${MAIL_USER} at ${MAIL_HOST} is used as owner no-replay won-owner-app-email-account

# owner
echo run owner container
docker ${docker_options} stop owner || echo 'No docker container found to stop with name: owner'
docker ${docker_options} rm owner || echo 'No docker container found to remove with name: owner'
docker ${docker_options} run --name=owner -d -e "node.default.host=$public_node_uri" \
-e "node.default.http.port=443" -p 8082:8443 \
-e "uri.host=$public_node_uri" -e "http.port=8082" \
-e "uri.prefix=https://$public_node_uri" \
-e "uri.prefix.node.default=https://${public_node_uri}/won" \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://${deploy_host}:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
webofneeds/owner:${deploy_image_tag_name}

# solr matcher
echo run solr matcher container
docker ${docker_options} stop matcher_solr || echo 'No docker container found to stop with name: matcher_solr'
docker ${docker_options} rm matcher_solr || echo 'No docker container found to remove with name: matcher_solr'
docker ${docker_options} run --name=matcher_solr -d -e "node.host=${deploy_host}" \
-e "cluster.seed.host=${deploy_host}" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-e "matcher.solr.uri.solr.server=http://${deploy_host}:8984/solr/" \
-e "matcher.solr.uri.solr.server.public=http://${deploy_host}:8984/solr/" \
-p 2562:2562 -e "JMEM_OPTS=-Xmx250m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/matcher_solr:${deploy_image_tag_name}
