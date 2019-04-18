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

# create the solr data directories (if not available yet) with full rights for every user.
# This is done so that the directory on the host can be written by the solr user from inside the container
ssh won@satvm01 mkdir -p $base_folder/solr/won/data
ssh won@satvm01 mkdir -p $base_folder/solr/wontest/data
ssh won@satvm01 chmod 777 $base_folder/solr/won/data
ssh won@satvm01 chmod 777 $base_folder/solr/wontest/data

# copy the matchat skin to the custom skin folder that get used by this instance
ssh won@satvm01 mkdir -p $base_folder/custom_owner_skin
scp -r $WORKSPACE/webofneeds/won-owner-webapp/src/main/webapp/skin/matchat/* won@satvm01:$base_folder/custom_owner_skin/

echo build the docker containers
docker --tlsverify -H satvm01.researchstudio.at:2376 pull webofneeds/bigdata
# TODO: change the explicit passing of tls params when docker-compose bug is fixed: https://github.com/docker/compose/issues/1427
cd deploy/live_satvm01
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm01.researchstudio.at:2376 build

echo run docker containers using docker-compose:
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm01.researchstudio.at:2376 down
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm01.researchstudio.at:2376 up -d

echo [currently deactivated:] push automatically built webobofatoms images to docker hub
# 
#docker --tlsverify -H satvm01.researchstudio.at:2376 login --username=$DOCKER_USER --password=$DOCKER_PASS
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/gencert:live
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/wonnode:live
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/owner:live
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/matcher_service:live
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/matcher_solr:live
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/matcher_rescal:live
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/solr:live
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/postgres:live
#docker --tlsverify -H satvm01.researchstudio.at:2376 push webofneeds/bots:live