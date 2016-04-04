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
  docker -H satcluster01:2375 stop postgres_ma || echo 'No docker container found to stop with name: postgres_ma'
  docker -H satcluster01:2375 rm postgres_ma || echo 'No docker container found to remove with name: postgres_ma'
fi

echo start docker build of images:
docker -H satcluster01:2375 build -t webofneeds/wonnode:master $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satcluster01:2375 build -t webofneeds/owner:master $WORKSPACE/webofneeds/won-docker/owner/
docker -H satcluster01:2375 build -t webofneeds/matcher_service:master $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satcluster01:2375 build -t webofneeds/matcher_siren:master $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satcluster01:2375 build -t webofneeds/gencert:master $WORKSPACE/webofneeds/won-docker/gencert/


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

# copy the nginx.conf file to the proxy server
rsync $WORKSPACE/webofneeds/won-docker/nginx/nginx.conf root@satcluster02:$base_folder/nginx.conf

echo run nginx proxy server
docker -H satcluster02:2375 pull webofneeds/nginx
if ! docker -H satcluster02:2375 run --name=nginx_ma -v $base_folder/won-server-certs:/etc/nginx/won-server-certs/ \
-v $base_folder/nginx.conf:/etc/nginx/nginx.conf -d -p 80:80 -p 443:443 nginx; then
  echo nginx container already available, restart old container
  docker -H satcluster02:2375 restart nginx_ma
fi

# run the script to start webofneeds containers on host satcluster01
public_node_uri=www.matchat.org
. $WORKSPACE/webofneeds/won-docker/deploy_master_run.sh

echo push automatically built webobofneeds images to docker hub
docker -H satcluster01:2375 login -u heikofriedrich
docker -H satcluster01:2375 push webofneeds/gencert:master
docker -H satcluster01:2375 push webofneeds/wonnode:master
docker -H satcluster01:2375 push webofneeds/owner:master
docker -H satcluster01:2375 push webofneeds/matcher_service:master
docker -H satcluster01:2375 push webofneeds/matcher_siren:master