# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
export base_folder=/usr/share/webofneeds/master
export live_base_folder=/home/won/matchat

# check if all application data should be removed before deployment
if [ "$remove_all_data" = true ] ; then

  echo generating new certificates! Old files will be deleted!
  ssh root@satvm02 rm -rf $base_folder/won-server-certsownerblue
  ssh root@satvm02 rm -rf $base_folder/won-server-certsownergreen
  ssh root@satvm02 rm -rf $base_folder/won-client-certs

  echo delete postgres, bigdata and solr databases!
  ssh root@satvm02 rm -rf $base_folder/postgresblue/data
  ssh root@satvm02 rm -rf $base_folder/postgresgreen/data
  ssh root@satvm02 rm -rf $base_folder/bigdata/data
  ssh root@satvm02 rm -rf $base_folder/solr/won/data
  ssh root@satvm02 rm -rf $base_folder/solr/wontest/data
  ssh root@satvm02 rm -rf $base_folder/mongodb/data
fi

ssh root@satvm02 mkdir -p $base_folder/won-server-certsownerblue
ssh root@satvm02 mkdir -p $base_folder/won-server-certsownergreen
ssh root@satvm02 mkdir -p $base_folder/won-client-certs

# create a password file for the certificates, variable ${won_certificate_passwd} must be set from outside the script
# note: name of the password file is fixed in won-docker/image/nginx/nginx.conf
echo ${won_certificate_passwd} > won_certificate_passwd_file
ssh root@satvm02 mkdir -p $base_folder/won-server-certsownerblue
scp won_certificate_passwd_file root@satvm02:$base_folder/won-server-certsownerblue/won_certificate_passwd_file
ssh root@satvm02 mkdir -p $base_folder/won-server-certsownergreen
scp won_certificate_passwd_file root@satvm02:$base_folder/won-server-certsownergreen/won_certificate_passwd_file
rm won_certificate_passwd_file

# create the solr data directories (if not available yet) with full rights for every user.
# This is done so that the directory on the host can be written by the solr user from inside the container
ssh root@satvm02 mkdir -p $base_folder/solr/won/data
ssh root@satvm02 mkdir -p $base_folder/solr/wontest/data
ssh root@satvm02 chmod 777 $base_folder/solr/won/data
ssh root@satvm02 chmod 777 $base_folder/solr/wontest/data

# copy the uki skin to the custom skin folder that get used by this instance
ssh root@satvm02 mkdir -p $base_folder/custom_owner_skin_blue
ssh root@satvm02 mkdir -p $base_folder/custom_owner_skin_green
scp -r $WORKSPACE/webofneeds/won-owner-webapp/src/main/webapp/static/skin/blue/* root@satvm02:$base_folder/custom_owner_skin_blue/
scp -r $WORKSPACE/webofneeds/won-owner-webapp/src/main/webapp/static/skin/green/* root@satvm02:$base_folder/custom_owner_skin_green/

# copy the openssl.conf file to the server where the certificates are generated
# the conf file is needed to specify alternative server names, see conf file in won-docker/image/gencert/openssl.conf
scp $WORKSPACE/webofneeds/won-docker/image/gencert/openssl.conf root@satvm02:$base_folder/openssl.conf

# copy letsencrypt certificate files from satvm01 (live/matchat) to satvm02
ssh root@satvm02 mkdir -p $base_folder/letsencrypt/certs/live/matchat.org
scp -3 won@satvm01:$live_base_folder/letsencrypt/certs/live/matchat.org/* root@satvm02:$base_folder/letsencrypt/certs/live/matchat.org/

# TODO: change the explicit passing of tls params when docker-compose bug is fixed: https://github.com/docker/compose/issues/1427
echo run docker containers using docker-compose on satvm02
docker --tlsverify -H satvm02.researchstudio.at:2376 pull webofneeds/bigdata
docker --tlsverify -H satvm02.researchstudio.at:2376 pull webofneeds/won-debugbot:latest
docker --tlsverify -H satvm02.researchstudio.at:2376 pull webofneeds/won-spoco-raidbot:latest
docker --tlsverify -H satvm02.researchstudio.at:2376 pull webofneeds/won-jobbot:latest
cd deploy/master_satvm02
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm02.researchstudio.at:2376 down
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm02.researchstudio.at:2376 up --build -d

