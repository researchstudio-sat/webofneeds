# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
export base_folder=/home/won/matchat

# create a password file for the certificates, variable ${won_certificate_passwd} must be set from outside the script
# note: name of the password file is fixed in won-docker/image/nginx/nginx.conf
echo ${won_certificate_passwd} > won_certificate_passwd_file
ssh won@satvm01 mkdir -p $base_folder/won-server-certs
scp won_certificate_passwd_file won@satvm01:$base_folder/won-server-certs/won_certificate_passwd_file
rm won_certificate_passwd_file

# copy the openssl.conf file to the server where the certificates are generated
# the conf file is needed to specify alternative server names, see conf file in won-docker/image/gencert/openssl.conf
# for entries of alternative server names: matchat.org, www.matchat.org, satvm01.researchstudio.at
scp $WORKSPACE/webofneeds/won-docker/image/gencert/openssl.conf won@satvm01:$base_folder/openssl.conf

# owner server certificate generator
# PLEASE NOTE that value of PASS should be the same used in your server.xml for SSLPassword on wonnode and owner,
# and the same as activemq.broker.keystore.password used in your wonnode activemq spring configurations for broker
# this image creates a keypair and a certificate based on it and stores the respective files as in the specified
# folder (see parameter of option '-v'). If different certificates are to be created for different images
# (e.g. owner and node), they should be put in different folders and these images should mount those different
# folders where needed (e.g. for tomcat, that folder is '/usr/local/tomcat/conf/ssl/', as used e.g. in the run
# command of the wonnode). Note that the filename of the certificate is also used in the tomcat config, (see
# owner/ssl/server.xml) so be careful when changing it.
docker -H satvm01:2375 rm gencert || echo 'No docker container found to remove with name: gencert'
docker -H satvm01:2375 run --name=gencert -e CN="matchat.org" \
-e "PASS=file:/usr/local/certs/out/won_certificate_passwd_file" -e "OPENSSL_CONFIG_FILE=/usr/local/openssl.conf" \
-v $base_folder/won-server-certs:/usr/local/certs/out/ \
-v $base_folder/openssl.conf:/usr/local/openssl.conf webofneeds/gencert:live

# copy the nginx.conf file to the proxy server
rsync $WORKSPACE/webofneeds/won-docker/image/nginx/nginx.conf won@satvm01:$base_folder/nginx.conf

echo build the docker containers
docker -H satvm01:2375 pull webofneeds/bigdata
cd deploy/live_satvm01
docker-compose -H satvm01:2375 build

echo run docker containers using docker-compose:
docker-compose -H satvm01:2375 down
docker-compose -H satvm01:2375 up -d

echo push automatically built webobofneeds images to docker hub
docker -H satvm01:2375 login --username=$DOCKER_USER --password=$DOCKER_PASS
docker -H satvm01:2375 push webofneeds/gencert:live
docker -H satvm01:2375 push webofneeds/wonnode:live
docker -H satvm01:2375 push webofneeds/owner:live
docker -H satvm01:2375 push webofneeds/matcher_service:live
docker -H satvm01:2375 push webofneeds/matcher_solr:live
docker -H satvm01:2375 push webofneeds/solr:live
docker -H satvm01:2375 push webofneeds/postgres:live
docker -H satvm01:2375 push webofneeds/bots:live