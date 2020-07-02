# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
echo exporting the basefolder
export base_folder=/usr/share/webofneeds/uki
echo basefolder export done: $base_folder
echo exporting the live_basefolder
export live_base_folder=/home/won/matchat
echo live_basefolder export done: live_base_folder


# create a password file for the certificates, variable ${uki_certificate_passwd} must be set from outside the script
# note: name of the password file is fixed in won-docker/image/nginx/nginx.conf
echo ${uki_certificate_passwd} > uki_certificate_passwd_file
ssh won@satvm06 mkdir -p $base_folder/won-server-certs
echo made directory $base_folder/won-server-certs
scp uki_certificate_passwd_file won@satvm06:$base_folder/won-server-certs/uki_certificate_passwd_file
echo copied uki_certificate_passwd_file to won@satvm06:$base_folder/won-server-certs/uki_certificate_passwd_file
rm uki_certificate_passwd_file
echo remove tmp passwd file

# copy the openssl.conf file to the server where the certificates are generated
# the conf file is needed to specify alternative server names, see conf file in won-docker/image/gencert/openssl.conf
# for entries of alternative server names: cbi.matchat.org, satvm06.researchstudio.at
scp $WORKSPACE/webofneeds/won-docker/image/gencert/openssl-uki.conf won@satvm06:$base_folder/openssl.conf
echo copied $WORKSPACE/webofneeds/won-docker/image/gencert/openssl-uki.conf to won@satvm06:$base_folder/openssl.conf

# copy the nginx.conf file to the proxy server
scp $WORKSPACE/webofneeds/won-docker/image/nginx/nginx-uki-http.conf won@satvm01:$live_base_folder/nginx-uki-http.conf
echo copied $WORKSPACE/webofneeds/won-docker/image/nginx/nginx-uki-http.conf to won@satvm06:$base_folder/nginx-uki-http.conf

# copy letsencrypt certificate files from satvm01 (live/matchat) to satvm06
ssh won@satvm06 mkdir -p $base_folder/letsencrypt/certs/live/cbi.matchat.org
echo made directory $base_folder/letsencrypt/certs/live/cbi.matchat.org
scp -3 -v won@satvm01:$live_base_folder/letsencrypt/certs/live/cbi.matchat.org/* won@satvm06:$base_folder/letsencrypt/certs/live/cbi.matchat.org/
echo copied won@satvm01:$live_base_folder/letsencrypt/certs/live/cbi.matchat.org/* to won@satvm06:$base_folder/letsencrypt/certs/live/cbi.matchat.org/

# create the solr data directories (if not available yet) with full rights for every user.
# This is done so that the directory on the host can be written by the solr user from inside the container
ssh won@satvm06 mkdir -p $base_folder/solr/won/data
echo made directory $base_folder/solr/won/data
ssh won@satvm06 mkdir -p $base_folder/solr/wontest/data
echo made directory $base_folder/solr/solr/wontest/data
ssh won@satvm06 chmod 777 $base_folder/solr/won/data
echo changed permissions for directory $base_folder/solr/won/data to 777
ssh won@satvm06 chmod 777 $base_folder/solr/wontest/data
echo changed permissions for directory $base_folder/solr/solr/wontest/data to 777

# copy the uki skin to the custom skin folder that get used by this instance
ssh won@satvm06 mkdir -p $base_folder/custom_owner_skin
echo made directory $base_folder/custom_owner_skin
scp -r $WORKSPACE/webofneeds/won-owner-webapp/src/main/webapp/skin/uki/* won@satvm06:$base_folder/custom_owner_skin/
echo copied $WORKSPACE/webofneeds/won-owner-webapp/src/main/webapp/skin/uki/* to won@satvm06:$base_folder/custom_owner_skin/

echo build the docker containers
docker --tlsverify -H satvm06.researchstudio.at:2376 pull webofneeds/bigdata
# TODO: change the explicit passing of tls params when docker-compose bug is fixed: https://github.com/docker/compose/issues/1427
cd deploy/uki_satvm06
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm06.researchstudio.at:2376 build

echo run docker containers using docker-compose:
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm06.researchstudio.at:2376 down
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm06.researchstudio.at:2376 up -d
