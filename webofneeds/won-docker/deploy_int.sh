#############################################################################################################
# Deployment descriptions:
# This script deploys several docker containers using docker-compose of the won application to the servers
# satsrv04, satsrv05 and satsrv06. The details of the container deployment can be found in the docker-compose
# files in the folder ./deploy/int_satsrv04 (int_satsrv05, int_satsrv06).
##############################################################################################################

# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
export base_folder=/home/install/won/int

# if the GENERATE_NEW_CERTIFICATES flag is set to true then setup things in a way that the certificates are recreated
# that (currently) includes:
# - deleting content of the server and client certificate folder
# - emptying the postgres database (all need data is lost!) => is done later in the script anyway
if [ "$GENERATE_NEW_CERTIFICATES" = true ] ; then
  echo generating new certificates! Old files and postgres need database will be deleted!
  ssh root@satsrv04 rm -rf $base_folder/won-server-certs
  ssh root@satsrv05 rm -rf $base_folder/won-server-certs
  ssh root@satsrv06 rm -rf $base_folder/won-server-certs
  ssh root@satsrv04 rm -rf $base_folder/won-client-certs
  ssh root@satsrv05 rm -rf $base_folder/won-client-certs
  ssh root@satsrv06 rm -rf $base_folder/won-client-certs
fi

# build the won docker images on every server of the cluster so that everywhere is the latest version available
echo start docker build of images:

ssh root@satsrv04 mkdir -p $base_folder/won-server-certs
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
ssh root@satsrv04 mkdir -p $base_folder/won-client-certs
ssh root@satsrv05 mkdir -p $base_folder/won-client-certs
ssh root@satsrv06 mkdir -p $base_folder/won-client-certs

# copy the openssl.conf file to the server where the certificates are generated
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
scp $WORKSPACE/webofneeds/won-docker/gencert/openssl-int.conf root@satsrv05:$base_folder/openssl-int.conf


echo run docker containers using docker-compose on satsrv04:
cd deploy/int_satsrv04
docker-compose -H satsrv04:2375 down
docker-compose -H satsrv04:2375 up -d

echo run docker containers using docker-compose on satsrv05:
cd ../int_satsrv05
docker-compose -H satsrv05:2375 down
docker-compose -H satsrv05:2375 up -d

# get the certificates and create a password file (for the nginx) to read the certificate
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
mkdir -p ~/won-server-certs
rm ~/won-server-certs/*
echo ${won_certificate_passwd} > ~/won-server-certs/won_certificate_passwd_file
rsync root@satsrv05:$base_folder/won-server-certs/* ~/won-server-certs/
rsync ~/won-server-certs/* root@satsrv06:$base_folder/won-server-certs/

# copy the nginx.conf file to the proxy server
rsync $WORKSPACE/webofneeds/won-docker/nginx/nginx-int.conf root@satsrv06:$base_folder/nginx-int.conf

echo run docker containers using docker-compose on satsrv06:
cd ../int_satsrv06
docker-compose -H satsrv06:2375 down
docker-compose -H satsrv06:2375 up -d






