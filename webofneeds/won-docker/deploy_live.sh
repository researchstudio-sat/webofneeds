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

# copy the nginx.conf file to the proxy server
scp $WORKSPACE/webofneeds/won-docker/image/nginx/nginx.conf won@satvm01:$base_folder/nginx.conf

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