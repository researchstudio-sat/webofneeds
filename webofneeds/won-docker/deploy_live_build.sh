# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
base_folder=/home/install

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
  docker -H satcluster01:2375 stop bigdata || echo 'No docker container found to stop with name: bigdata'
  docker -H satcluster01:2375 rm bigdata || echo 'No docker container found to remove with name: bigdata'
  docker -H satcluster01:2375 stop solr || echo 'No docker container found to stop with name: solr'
  docker -H satcluster01:2375 rm solr || echo 'No docker container found to remove with name: solr'
fi

echo start docker build of images:
docker -H satcluster01:2375 build -t webofneeds/wonnode:live $WORKSPACE/webofneeds/won-docker/wonnode/
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

# wonnode/owner server certificate generator
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
rm ~/won-server-certs/*
rsync root@satcluster01:$base_folder/won-server-certs/* ~/won-server-certs/
rsync ~/won-server-certs/* root@satcluster02:$base_folder/won-server-certs/

# copy the nginx.conf file to the proxy server
rsync $WORKSPACE/webofneeds/won-docker/nginx/nginx.conf root@satcluster02:$base_folder/nginx.conf
rsync $WORKSPACE/webofneeds/won-docker/nginx/nginx-http-only.conf root@satcluster02:$base_folder/nginx-http-only.conf


# start the letsencrypt ssl certificate request cron job container, executes cron job requests/renewals every 12 hours
docker -H satcluster02:2375 stop letsencrypt || echo 'No docker container found to stop with name: letsencrypt'
docker -H satcluster02:2375 rm letsencrypt || echo 'No docker container found to remove with name: letsencrypt'
docker -H satcluster02:2375 run --name=letsencrypt -d -v /home/install/letsencrypt/acme-challenge:/usr/share/nginx/html/ \
-v /home/install/letsencrypt/certs:/etc/letsencrypt/ webofneeds/letsencrypt:live

# bootstrap code: if no nginx is running yet start it up in htt-only mode to generate a first letsencrypt certificate.
# Then shut it down and start the nginx in normal (ssl) mode with the letsencrypt certificate
if docker -H satcluster02:2375 run --name=nginx_http_only \
-v /home/install/nginx-http-only.conf:/etc/nginx/nginx.conf \
-v /home/install/letsencrypt/acme-challenge:/usr/share/nginx/html/ -d -p 80:80 nginx; then
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
. $WORKSPACE/webofneeds/won-docker/deploy_live_run.sh

echo push automatically built webobofneeds images to docker hub
docker -H satcluster01:2375 login -u heikofriedrich
docker -H satcluster01:2375 push webofneeds/gencert:live
docker -H satcluster01:2375 push webofneeds/wonnode:live
docker -H satcluster01:2375 push webofneeds/owner:live
docker -H satcluster01:2375 push webofneeds/matcher_service:live
docker -H satcluster01:2375 push webofneeds/matcher_solr:live
docker -H satcluster01:2375 push webofneeds/letsencrypt:live