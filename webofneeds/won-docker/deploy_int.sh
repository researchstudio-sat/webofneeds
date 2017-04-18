#############################################################################################################
# Deployment descriptions:
# This script deploys several docker containers using docker-compose of the won application to the servers
# satsrv04, satsrv05 and satsrv06. The details of the container deployment can be found in the docker-compose
# files in the folder ./deploy/int_satsrv04 (int_satsrv05, int_satsrv06).
##############################################################################################################

# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
export base_folder=/usr/share/webofneeds/int

# check if all application data should be removed before deployment
if [ "$remove_all_data" = true ] ; then

  echo generating new certificates! Old files will be deleted!
  ssh root@satsrv04 rm -rf $base_folder/won-server-certs
  ssh root@satsrv05 rm -rf $base_folder/won-server-certs
  ssh root@satsrv06 rm -rf $base_folder/won-server-certs
  ssh root@satsrv04 rm -rf $base_folder/won-client-certs
  ssh root@satsrv05 rm -rf $base_folder/won-client-certs
  ssh root@satsrv06 rm -rf $base_folder/won-client-certs
  rm -rf $base_folder/won-server-certs

  echo delete postgres, bigdata, mongodb and solr databases!
  ssh root@satsrv04 rm -rf $base_folder/postgres/data
  ssh root@satsrv05 rm -rf $base_folder/postgres/data
  ssh root@satsrv06 rm -rf $base_folder/bigdata/data
  ssh root@satsrv06 rm -rf $base_folder/solr/won/data
  ssh root@satsrv06 rm -rf $base_folder/solr/wontest/data
  ssh root@satsrv06 rm -rf $base_folder/mongodb/data
fi

ssh root@satsrv04 mkdir -p $base_folder/won-server-certs
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
ssh root@satsrv04 mkdir -p $base_folder/won-client-certs
ssh root@satsrv05 mkdir -p $base_folder/won-client-certs
ssh root@satsrv06 mkdir -p $base_folder/won-client-certs

# copy the openssl.conf file to the server where the certificates are generated
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
scp $WORKSPACE/webofneeds/won-docker/image/gencert/openssl-int.conf root@satsrv05:$base_folder/openssl-int.conf

# create the solr data directories (if not available yet) with full rights for every user.
# This is done so that the directory on the host can be written by the solr user from inside the container
ssh root@satsrv06 mkdir -p $base_folder/solr/won/data
ssh root@satsrv06 mkdir -p $base_folder/solr/wontest/data
ssh root@satsrv06 chmod 777 $base_folder/solr/won/data
ssh root@satsrv06 chmod 777 $base_folder/solr/wontest/data

echo run docker containers using docker-compose on satsrv04:
cd deploy/int_satsrv04
docker-compose -H satsrv04:2375 down
docker-compose -H satsrv04:2375 up --build -d

echo run docker containers using docker-compose on satsrv05:
cd ../int_satsrv05
docker-compose -H satsrv05:2375 down
docker-compose -H satsrv05:2375 up --build -d

# get the certificates and create a password file (for the nginx) to read the certificate
# the certificates must have been created on satsrv05 (in docker-compose file) before it can be used on proxy satsrv06
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
mkdir -p ~/won-server-certs
rm -f ~/won-server-certs/*
echo ${won_certificate_passwd} > ~/won-server-certs/won_certificate_passwd_file
rsync root@satsrv05:$base_folder/won-server-certs/* ~/won-server-certs/
rsync ~/won-server-certs/* root@satsrv06:$base_folder/won-server-certs/

# copy the nginx.conf file to the proxy server
scp $WORKSPACE/webofneeds/won-docker/image/nginx/nginx-int.conf root@satsrv06:$base_folder/nginx-int.conf

echo run docker containers using docker-compose on satsrv06:
cd ../int_satsrv06
docker -H satsrv06:2375 pull webofneeds/bigdata
docker-compose -H satsrv06:2375 down
docker-compose -H satsrv06:2375 up --build -d






