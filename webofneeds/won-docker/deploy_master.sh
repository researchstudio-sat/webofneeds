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
export base_folder=/home/install/won/master
jenkins_base_folder=/var/lib/jenkins/won/master
mkdir -p $jenkins_base_folder

# if the GENERATE_NEW_CERTIFICATES flag is set to true then setup things in a way that the certificates are recreated
# that (currently) includes:
# - deleting content of the server and client certificate folder
# - emptying the postgres database (all need data is lost!)
if [ "$GENERATE_NEW_CERTIFICATES" = true ] ; then
  echo generating new certificates! Old files and postgres need database will be deleted!
  ssh root@satsrv04 rm -rf $base_folder/won-server-certs
  ssh root@satsrv05 rm -rf $base_folder/won-server-certs
  ssh root@satsrv06 rm -rf $base_folder/won-server-certs
  ssh root@satsrv04 rm -rf $base_folder/won-client-certs
  ssh root@satsrv05 rm -rf $base_folder/won-client-certs
  ssh root@satsrv06 rm -rf $base_folder/won-client-certs
  rm -rf $jenkins_base_folder/won-server-certs

  # TODO: delete data

fi

ssh root@satsrv04 mkdir -p $base_folder/won-server-certs
ssh root@satsrv05 mkdir -p $base_folder/won-server-certs
ssh root@satsrv06 mkdir -p $base_folder/won-server-certs
ssh root@satsrv04 mkdir -p $base_folder/won-client-certs
ssh root@satsrv05 mkdir -p $base_folder/won-client-certs
ssh root@satsrv06 mkdir -p $base_folder/won-client-certs
mkdir -p $jenkins_base_folder/won-server-certs

# copy the openssl.conf file to the server where the certificates are generated
scp $WORKSPACE/webofneeds/won-docker/gencert/openssl-master.conf root@satsrv05:$base_folder/openssl-master.conf

echo run docker containers using docker-compose on satsrv07:
cd deploy/int_satsrv07
docker-compose -H satsrv07:2375 down
docker-compose -H satsrv07:2375 up -d

echo run docker containers using docker-compose on satsrv04:
cd ../int_satsrv04
docker-compose -H satsrv04:2375 down
docker-compose -H satsrv04:2375 up -d

echo run docker containers using docker-compose on satsrv05:
cd ../int_satsrv05
docker-compose -H satsrv05:2375 down
docker-compose -H satsrv05:2375 up -d

# get the certificates and create a password file (for the nginx) to read the certificate
echo ${won_certificate_passwd} > $jenkins_base_folder/won-server-certs/won_certificate_passwd_file
rsync root@satsrv05:$base_folder/won-server-certs/* $jenkins_base_folder/won-server-certs/

echo run docker containers using docker-compose on satsrv06:
cd ../int_satsrv06
docker-compose -H satsrv06:2375 down
docker-compose -H satsrv06:2375 up -d


# if everything works up to this point - build :master images locally and push these local images into the dockerhub:
# build:
docker -H localhost:2375 build -t webofneeds/gencert:master $WORKSPACE/webofneeds/won-docker/gencert/
docker -H localhost:2375 build -t webofneeds/wonnode:master $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H localhost:2375 build -t webofneeds/owner:master $WORKSPACE/webofneeds/won-docker/owner/
docker -H localhost:2375 build -t webofneeds/matcher_service:master $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H localhost:2375 build -t webofneeds/matcher_solr:master $WORKSPACE/webofneeds/won-docker/matcher-solr/
# push:
docker -H localhost:2375 login -u heikofriedrich
docker -H localhost:2375 push webofneeds/gencert:master
docker -H localhost:2375 push webofneeds/wonnode:master
docker -H localhost:2375 push webofneeds/owner:master
docker -H localhost:2375 push webofneeds/matcher_service:master
docker -H localhost:2375 push webofneeds/matcher_solr:master
