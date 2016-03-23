# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
base_folder=/home/install

# if the GENERATE_NEW_CERTIFICATES flag is set to true then setup things in a way that the certificates are recreated
# that (currently) includes:
# - deleting content of the server and client certificate folder => the content of the whole base_folder is deleted!
# - emptying the postgres database (all need data is lost!)
if [ "$GENERATE_NEW_CERTIFICATES" = true ] ; then
  echo generating new certificates! Old files and postgres need database will be deleted!
  ssh root@satcluster01 rm -rf $base_folder/won-server-certs/*
  ssh root@satcluster01 rm -rf $base_folder/won-client-certs/*
  docker -H satcluster01:2375 stop postgres_ma || echo 'No docker container found to stop with name: postgres_ma'
  docker -H satcluster01:2375 rm postgres_ma || echo 'No docker container found to remove with name: postgres_ma'
fi

echo start docker build of images:
docker -H satcluster01:2375 build -t webofneeds/wonnode:master $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satcluster01:2375 build -t webofneeds/owner:master $WORKSPACE/webofneeds/won-docker/owner/
docker -H satcluster01:2375 build -t webofneeds/matcher_service:master $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satcluster01:2375 build -t webofneeds/matcher_siren:master $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satcluster01:2375 build -t webofneeds/gencert:master $WORKSPACE/webofneeds/won-docker/gencert/


# start the won containers on dedicated servers of the cluster
echo run docker containers:

# NOTE: do not redeploy the postgres database for won node and owner to keep the data after deployments
# postgres db
docker -H satcluster01:2375 pull webofneeds/postgres
echo try to start new postgres container
if ! docker -H satcluster01:2375 run --name=postgres_ma -d -p 5433:5432 webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker -H satcluster01:2375 restart postgres_ma
fi

sleep 10

# create a password file for the certificates, variable ${won_certificate_passwd} must be set from outside the script
# note: name of the password file is fixed in won-docker/nginx/nginx.conf
echo ${won_certificate_passwd} > won_certificate_passwd_file
ssh root@satcluster01 mkdir -p $base_folder/won-server-certs
scp won_certificate_passwd_file root@satcluster01:$base_folder/won-server-certs/won_certificate_passwd_file
rm won_certificate_passwd_file

# copy the openssl.conf file to the server where the certificates are generated
# the conf file is needed to specify alternative server names, see conf file in won-docker/gencert/openssl.conf for
# entries of alternative server names: www.matchat.org, satcluster01.researchstudio.at, satcluster02.researchstudio.at
scp $WORKSPACE/webofneeds/won-docker/gencert/openssl.conf root@satcluster01:$base_folder/openssl.conf

# wonnode/owner server certificate generator
# PLEASE NOTE that value of PASS should be the same used in your server.xml for SSLPassword on wonnode and owner,
# and the same as activemq.broker.keystore.password used in your wonnode activemq spring configurations for broker
# this image creates a keypair and a certificate based on it and stores the respective files as in the specified
# folder (see parameter of option '-v'). If different certificates are to be created for different images
# (e.g. owner and node), they should be put in different folders and these images should mount those different
# folders where needed (e.g. for tomcat, that folder is '/usr/local/tomcat/conf/ssl/', as used e.g. in the run
# command of the wonnode). Note that the filename of the certificate is also used in the tomcat config, (see
# owner/ssl/server.xml) so be careful when changing it.
docker -H satcluster01:2375 rm gencert_ma || echo 'No docker container found to remove with name: gencert_ma'
docker -H satcluster01:2375 run --name=gencert_ma -e CN="www.matchat.org" \
-e "PASS=file:/usr/local/certs/out/won_certificate_passwd_file" -e "OPENSSL_CONFIG_FILE=/usr/local/openssl.conf" \
-v $base_folder/won-server-certs:/usr/local/certs/out/ \
-v $base_folder/openssl.conf:/usr/local/openssl.conf webofneeds/gencert:master

# copy the server certificates (and password file) to the proxy server and start the nginx container
# if it is not already started
ssh root@satcluster02 mkdir -p $base_folder/won-server-certs
mkdir -p ~/won-server-certs
rm ~/won-server-certs/*
rsync root@satcluster01:$base_folder/won-server-certs/* ~/won-server-certs/
rsync ~/won-server-certs/* root@satcluster02:$base_folder/won-server-certs/

docker -H satcluster02:2375 pull webofneeds/nginx
if ! docker -H satcluster02:2375 run --name=nginx_ma -v $base_folder/won-server-certs:/etc/nginx/won-server-certs/ \
-d -p 80:80 -p 443:443 webofneeds/nginx; then
  echo nginx container already available, restart old container
  docker -H satcluster02:2375 restart nginx_ma
fi

sleep 5


# wonnode
docker -H satcluster01:2375 stop wonnode_ma || echo 'No docker container found to stop with name: wonnode_ma'
docker -H satcluster01:2375 rm wonnode_ma || echo 'No docker container found to remove with name: wonnode_ma'
docker -H satcluster01:2375 run --name=wonnode_ma -d -e "uri.host=satcluster01.researchstudio.at" -e "http.port=8889" -e \
"activemq.broker.port=61617" -p 8889:8443 -p 61617:61617 \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode_ma:/usr/local/tomcat/won/client-certs/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satcluster01:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-e "JMEM_OPTS=-Xmx400m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/wonnode:master

# expect OWNER won-mail-sender host, user and password (i.e. configuration for no-replay won-owner-app-email-account) be
# set as environment variables, e.g. MAIL_USER=changeuser MAIL_PASS=changepass MAIL_HOST=smtp.changehost.com
echo ${MAIL_USER} at ${MAIL_HOST} is used as owner no-replay won-owner-app-email-account

# owner
docker -H satcluster01:2375 stop owner_ma || echo 'No docker container found to stop with name: owner_ma'
docker -H satcluster01:2375 rm owner_ma || echo 'No docker container found to remove with name: owner_ma'
docker -H satcluster01:2375 run --name=owner_ma -d -e "node.default.host=satcluster01.researchstudio.at" \
-e "node.default.http.port=8889" -p 8082:8443 \
-e "uri.host=satcluster01.researchstudio.at" -e "http.port=8082" \
-e "email.from.won.user=${MAIL_USER}" -e "email.from.won.password=${MAIL_PASS}" -e "email.from.won.smtp.host=${MAIL_HOST}" \
-v $base_folder/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/owner_ma:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satcluster01:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
webofneeds/owner:master


# bigdata
docker -H satcluster01:2375 pull webofneeds/bigdata
docker -H satcluster01:2375 stop bigdata_ma || echo 'No docker container found to stop with name: bigdata_ma'
docker -H satcluster01:2375 rm bigdata_ma || echo 'No docker container found to remove with name: bigdata_ma'
docker -H satcluster01:2375 run --name=bigdata_ma -d -p 10000:9999 \
-m 400m webofneeds/bigdata

# matcher service
docker -H satcluster01:2375 stop matcher_service_ma || echo 'No docker container found to stop with name: matcher_service_ma'
docker -H satcluster01:2375 rm matcher_service_ma || echo 'No docker container found to remove with name: matcher_service_ma'
docker -H satcluster01:2375 run --name=matcher_service_ma -d -e "node.host=satcluster01.researchstudio.at" \
-e "cluster.seed.host=satcluster01.researchstudio.at" \
-e "uri.sparql.endpoint=http://satcluster01.researchstudio.at:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://satcluster01.researchstudio.at:8889/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -p 2561:2561 \
-v $base_folder/won-client-certs/matcher_service_ma:/usr/src/matcher-service/client-certs/ \
-e "JMEM_OPTS=-Xmx250m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/matcher_service:master

# siren solr server
docker -H satcluster01:2375 pull webofneeds/sirensolr
docker -H satcluster01:2375 stop sirensolr_ma || echo 'No docker container found to stop with name: sirensolr_ma'
docker -H satcluster01:2375 rm sirensolr_ma || echo 'No docker container found to remove with name: sirensolr_ma'
docker -H satcluster01:2375 run --name=sirensolr_ma -d -p 7071:8080 -p 8984:8983 --env CATALINA_OPTS="-Xmx200m \
-XX:MaxPermSize=150m -XX:+HeapDumpOnOutOfMemoryError" webofneeds/sirensolr

sleep 10

# siren matcher
docker -H satcluster01:2375 stop matcher_siren_ma || echo 'No docker container found to stop with name: matcher_siren_ma'
docker -H satcluster01:2375 rm matcher_siren_ma || echo 'No docker container found to remove with name: matcher_siren_ma'
docker -H satcluster01:2375 run --name=matcher_siren_ma -d -e "node.host=satcluster01.researchstudio.at" \
-e "cluster.seed.host=satcluster01.researchstudio.at" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-e "matcher.siren.uri.solr.server=http://satcluster01.researchstudio.at:8984/solr/won/" \
-e "matcher.siren.uri.solr.server.public=http://satcluster01.researchstudio.at:8984/solr/#/won/" \
-p 2562:2562 \
-e "JMEM_OPTS=-Xmx250m -XX:MaxMetaspaceSize=200m -XX:+HeapDumpOnOutOfMemoryError" \
webofneeds/matcher_siren:master


# if everything works up to this point push these local images into the dockerhub:
docker -H satcluster01:2375 login -u heikofriedrich
docker -H satcluster01:2375 push webofneeds/gencert:master
docker -H satcluster01:2375 push webofneeds/wonnode:master
docker -H satcluster01:2375 push webofneeds/owner:master
docker -H satcluster01:2375 push webofneeds/matcher_service:master
docker -H satcluster01:2375 push webofneeds/matcher_siren:master
