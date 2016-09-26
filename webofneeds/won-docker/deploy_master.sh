#############################################################################################################
# Deployment descriptions:
# This script deploys several docker containers using docker-compose of the won application to the servers
# satsrv04, satsrv05, satsrv06 and satsrv07. The details of the container deployment can be found in the docker-compose
# files in the folder ./deploy/master_satsrv04 (master_satsrv05, master_satsrv06, master_satsrv07 ).
#
# The data in the databases (postgres), rdf-stores (bigdata) and indices (solr) are kept between deployments
# and are only deleted if the certificate changes and the postgres db has to be recreated.
##############################################################################################################

# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
export base_folder=/usr/share/webofneeds/master
mkdir -p $base_folder

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

  echo delete postgres, bigdata and solr databases!
  ssh root@satsrv04 rm -rf $base_folder/postgres/data
  ssh root@satsrv05 rm -rf $base_folder/postgres/data
  ssh root@satsrv06 rm -rf $base_folder/bigdata/data
  ssh root@satsrv06 rm -rf $base_folder/solr/won/data
  ssh root@satsrv06 rm -rf $base_folder/solr/wontest/data
fi

ssh root@satsrv04 mkdir -p $base_folder/won-server-certs
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
ssh root@satsrv04 mkdir -p $base_folder/won-client-certs
ssh root@satsrv05 mkdir -p $base_folder/won-client-certs
ssh root@satsrv06 mkdir -p $base_folder/won-client-certs
mkdir -p $base_folder/won-server-certs

# copy the openssl.conf file to the server where the certificates are generated
scp $WORKSPACE/webofneeds/won-docker/image/gencert/openssl-master.conf root@satsrv05:$base_folder/openssl-master.conf

# copy the nginx.conf file to the proxy server
cp $WORKSPACE/webofneeds/won-docker/image/nginx/nginx-master.conf $base_folder/nginx-master.conf

echo run docker containers using docker-compose on satsrv04:
cd deploy/master_satsrv04
docker-compose -H satsrv04:2375 down
docker-compose -H satsrv04:2375 up -d

echo run docker containers using docker-compose on satsrv05:
cd ../master_satsrv05
docker-compose -H satsrv05:2375 down
docker-compose -H satsrv05:2375 up -d

# get the certificates and create a password file (for the nginx) to read the certificate
# the certificates must have been created on satsrv05 (in docker-compose file) before it can be used on proxy satsrv07
echo ${won_certificate_passwd} > $base_folder/won-server-certs/won_certificate_passwd_file
rsync root@satsrv05:$base_folder/won-server-certs/* $base_folder/won-server-certs/

echo run docker containers using docker-compose on satsrv07:
cd ../master_satsrv07
docker-compose -H satsrv07:2375 down
docker-compose -H satsrv07:2375 up -d

echo run docker containers using docker-compose on satsrv06:
cd ../master_satsrv06
docker -H satsrv06:2375 pull webofneeds/bigdata
docker-compose -H satsrv06:2375 down
docker-compose -H satsrv06:2375 up -d

