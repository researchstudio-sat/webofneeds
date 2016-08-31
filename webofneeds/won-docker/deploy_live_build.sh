# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
base_folder=/home/install

#won_certificate_passwd=changeit
#WORKSPACE=/var/lib/jenkins/jobs/webofneeds-integration-test/workspace
#GENERATE_NEW_CERTIFICATES=true
#MAIL_USER=
#MAIL_PASS=
#MAIL_HOST=


echo build webofneeds images and create certificates

# if the GENERATE_NEW_CERTIFICATES flag is set to true then setup things in a way that the certificates are recreated
# that (currently) includes:
# - deleting content of the server and client certificate folder => the content of the whole base_folder is deleted!
# - emptying the postgres database (all need data is lost!)
if [ "$GENERATE_NEW_CERTIFICATES" = true ] ; then
  echo generating new certificates! Old files and postgres need database will be deleted!
  ssh root@satcluster01 rm -rf $base_folder/won-server-certs/*
  ssh root@satcluster01 rm -rf $base_folder/won-client-certs/*
  docker -H satcluster01:2375 stop postgres || echo 'No docker container found to stop with name: postgres'
  docker -H satcluster01:2375 rm postgres || echo 'No docker container found to remove with name: postgres'
  docker -H satcluster02:2375 stop postgres || echo 'No docker container found to stop with name: postgres'
  docker -H satcluster02:2375 rm postgres || echo 'No docker container found to remove with name: postgres'
  docker -H satcluster01:2375 stop bigdata || echo 'No docker container found to stop with name: bigdata'
  docker -H satcluster01:2375 rm bigdata || echo 'No docker container found to remove with name: bigdata'
  docker -H satcluster01:2375 stop solr || echo 'No docker container found to stop with name: solr'
  docker -H satcluster01:2375 rm solr || echo 'No docker container found to remove with name: solr'
fi

echo start docker build of images:
docker -H satcluster02:2375 build -t webofneeds/wonnode:live $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satcluster01:2375 build -t webofneeds/owner:live $WORKSPACE/webofneeds/won-docker/owner/
docker -H satcluster01:2375 build -t webofneeds/matcher_service:live $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satcluster01:2375 build -t webofneeds/matcher_solr:live $WORKSPACE/webofneeds/won-docker/matcher-solr/
docker -H satcluster01:2375 build -t webofneeds/gencert:live $WORKSPACE/webofneeds/won-docker/gencert/
docker -H satcluster02:2375 build -t webofneeds/letsencrypt:live $WORKSPACE/webofneeds/won-docker/letsencrypt/


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

# owner server certificate generator
# PLEASE NOTE that value of PASS should be the same used in your server.xml for SSLPassword on wonnode and owner,
# and the same as activemq.broker.keystore.password used in your wonnode activemq spring configurations for broker
# this image creates a keypair and a certificate based on it and stores the respective files as in the specified
# folder (see parameter of option '-v'). If different certificates are to be created for different images
# (e.g. owner and node), they should be put in different folders and these images should mount those different
# folders where needed (e.g. for tomcat, that folder is '/usr/local/tomcat/conf/ssl/', as used e.g. in the run
# command of the wonnode). Note that the filename of the certificate is also used in the tomcat config, (see
# owner/ssl/server.xml) so be careful when changing it.
docker -H satcluster01:2375 rm gencert || echo 'No docker container found to remove with name: gencert'
docker -H satcluster01:2375 run --name=gencert -e CN="www.matchat.org" \
-e "PASS=file:/usr/local/certs/out/won_certificate_passwd_file" -e "OPENSSL_CONFIG_FILE=/usr/local/openssl.conf" \
-v $base_folder/won-server-certs:/usr/local/certs/out/ \
-v $base_folder/openssl.conf:/usr/local/openssl.conf webofneeds/gencert:live

# copy the server certificates (and password file) to the proxy server and start the nginx container
# if it is not already started
ssh root@satcluster02 mkdir -p $base_folder/won-server-certs
mkdir -p ~/won-server-certs
rm -f ~/won-server-certs/*
rsync root@satcluster01:$base_folder/won-server-certs/* ~/won-server-certs/
rsync ~/won-server-certs/* root@satcluster02:$base_folder/won-server-certs/

# copy the nginx.conf file to the proxy server
rsync $WORKSPACE/webofneeds/won-docker/nginx/nginx.conf root@satcluster02:$base_folder/nginx.conf
rsync $WORKSPACE/webofneeds/won-docker/nginx/nginx-http-only.conf root@satcluster02:$base_folder/nginx-http-only.conf


# start the letsencrypt ssl certificate request cron job container, executes cron job requests/renewals every 12 hours
docker -H satcluster02:2375 stop letsencrypt || echo 'No docker container found to stop with name: letsencrypt'
docker -H satcluster02:2375 rm letsencrypt || echo 'No docker container found to remove with name: letsencrypt'
docker -H satcluster02:2375 run --name=letsencrypt -d -v $base_folder/letsencrypt/acme-challenge:/usr/share/nginx/html/ \
-v $base_folder/letsencrypt/certs:/etc/letsencrypt/ -e "key_store_password=${won_certificate_passwd}" webofneeds/letsencrypt:live

# bootstrap code: if no nginx is running yet start it up in htt-only mode to generate a first letsencrypt certificate.
# Then shut it down and start the nginx in normal (ssl) mode with the letsencrypt certificate
if docker -H satcluster02:2375 run --name=nginx_http_only \
-v $base_folder/nginx-http-only.conf:/etc/nginx/nginx.conf \
-v $base_folder/letsencrypt/acme-challenge:/usr/share/nginx/html/ -d -p 80:80 nginx; then
  echo bootstrap: no nginx proxy was running on this server, started nginx in http-only mode and try to retrieve an letsencrypt certificate

  # start the letsencrypt container with CMD to execute the certificate challenge once
  docker -H satcluster02:2375 exec letsencrypt bash //usr/local/bin/certificate-request-and-renew.sh
fi

# stop and remove the nginx http-only container if it is there
docker -H satcluster02:2375 stop nginx_http_only || echo 'No docker container found to stop with name: nginx_http_only'
docker -H satcluster02:2375 rm nginx_http_only || echo 'No docker container found to remove with name: nginx_http_only'

echo run nginx proxy server with letsencrypt ssl certificate
if ! docker -H satcluster02:2375 run --name=nginx -v $base_folder/letsencrypt/certs:/etc/letsencrypt/ \
-v $base_folder/nginx.conf:/etc/nginx/nginx.conf \
-v $base_folder/letsencrypt/acme-challenge:/usr/share/nginx/html/ \
-d -p 80:80 -p 443:443 -p 61617:61617 nginx; then
  echo nginx container already available, restart old container
  docker -H satcluster02:2375 restart nginx
fi

# run the script to start webofneeds containers on host satcluster01
docker_options="-H satcluster01:2375"
public_node_uri=www.matchat.org
behind_proxy=true
deploy_image_tag_name=live


# =====================================================================
# deploy application containers
# =====================================================================

echo "deployment options: "
echo "docker_options: $docker_options"
echo "deploy_image_tag_name: $deploy_image_tag_name"
echo "public_node_uri: $public_node_uri"
echo "behind_proxy: $behind_proxy"
echo "base_folder: $base_folder"

# start the won containers on dedicated servers of the cluster
echo run docker containers:

# postgres
# NOTE: do not redeploy the postgres database for won node and owner to keep the data after deployments postgres db
echo run postgres containers
docker -H satcluster01:2375 pull webofneeds/postgres
if ! docker -H satcluster01:2375 run --name=postgres -d -p 5433:5432 webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker -H satcluster01:2375 restart postgres
fi

docker -H satcluster02:2375 pull webofneeds/postgres
if ! docker -H satcluster02:2375 run --name=postgres -d -p 5433:5432 webofneeds/postgres; then
  echo postgres container already available, restart old container
  docker -H satcluster02:2375 restart postgres
fi


sleep 10

# wonnode
echo run wonnode container
docker -H satcluster02:2375 stop wonnode || echo 'No docker container found to stop with name: wonnode'
docker -H satcluster02:2375 rm wonnode || echo 'No docker container found to remove with name: wonnode'
docker -H satcluster02:2375 run --name=wonnode -d -e "uri.host=$public_node_uri" -e "http.port=8443" \
-e "uri.prefix=https://${public_node_uri}/won" \
-e "activemq.broker.port=61616" -p 8443:8443 -p 61616:61616 \
-v $base_folder/letsencrypt/certs/live/www.matchat.org/fullchain.pem:/usr/local/tomcat/conf/ssl/t-cert.pem \
-v $base_folder/letsencrypt/certs/live/www.matchat.org/privkey.pem:/usr/local/tomcat/conf/ssl/t-key.pem \
-v $base_folder/letsencrypt/certs:/usr/local/tomcat/conf/ssl/ \
-v $base_folder/won-client-certs/wonnode:/usr/local/tomcat/won/client-certs/ \
-e "CERTIFICATE_PASSWORD=${won_certificate_passwd}" \
-e "client.authentication.behind.proxy=$behind_proxy" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satcluster02.researchstudio.at:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
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
docker ${docker_options} run --name=matcher_service -d -e "node.host=satcluster01.researchstudio.at" \
-e "cluster.seed.host=satcluster01.researchstudio.at" \
-e "uri.sparql.endpoint=http://satcluster01.researchstudio.at:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://${public_node_uri}/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -p 2561:2561 \
-v $base_folder/won-client-certs/matcher_service:/usr/src/matcher-service/client-certs/ \
webofneeds/matcher_service:${deploy_image_tag_name}

# solr server
echo run solr server container
docker ${docker_options} pull webofneeds/solr
if ! docker ${docker_options} run --name=solr -d -p 7071:8080 -p 8984:8983 webofneeds/solr; then
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
-e "db.sql.jdbcUrl=jdbc:postgresql://satcluster01.researchstudio.at:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
webofneeds/owner:${deploy_image_tag_name}

# solr matcher
echo run solr matcher container
docker ${docker_options} stop matcher_solr || echo 'No docker container found to stop with name: matcher_solr'
docker ${docker_options} rm matcher_solr || echo 'No docker container found to remove with name: matcher_solr'
docker ${docker_options} run --name=matcher_solr -d -e "node.host=satcluster01.researchstudio.at" \
-e "cluster.seed.host=satcluster01.researchstudio.at" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-e "matcher.solr.uri.solr.server=http://satcluster01.researchstudio.at:8984/solr/" \
-e "matcher.solr.uri.solr.server.public=http://satcluster01.researchstudio.at:8984/solr/" \
-p 2562:2562 webofneeds/matcher_solr:${deploy_image_tag_name}


# =====================================================================

echo push automatically built webobofneeds images to docker hub
docker -H satcluster01:2375 login -u heikofriedrich
docker -H satcluster01:2375 push webofneeds/gencert:live
docker -H satcluster02:2375 push webofneeds/wonnode:live
docker -H satcluster01:2375 push webofneeds/owner:live
docker -H satcluster01:2375 push webofneeds/matcher_service:live
docker -H satcluster01:2375 push webofneeds/matcher_solr:live
docker -H satcluster02:2375 push webofneeds/letsencrypt:live