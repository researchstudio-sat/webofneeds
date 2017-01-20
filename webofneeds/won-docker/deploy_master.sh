# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
export base_folder=/usr/share/webofneeds/master
mkdir -p $base_folder

# check if all application data should be removed before deployment
if [ "$remove_all_data" = true ] ; then

  echo generating new certificates! Old files will be deleted!
  ssh root@satvm02 rm -rf $base_folder/won-server-certs1
  ssh root@satvm02 rm -rf $base_folder/won-server-certs2
  ssh root@satvm02 rm -rf $base_folder/won-client-certs

  echo delete postgres, bigdata and solr databases!
  ssh root@satvm02 rm -rf $base_folder/postgres1/data
  ssh root@satvm02 rm -rf $base_folder/postgres2/data
  ssh root@satvm02 rm -rf $base_folder/bigdata/data
  ssh root@satvm02 rm -rf $base_folder/solr/won/data
  ssh root@satvm02 rm -rf $base_folder/solr/wontest/data
  ssh root@satvm02 rm -rf $base_folder/mongodb/data
fi

ssh root@satvm02 mkdir -p $base_folder/won-server-certs1
ssh root@satvm02 mkdir -p $base_folder/won-server-certs2
ssh root@satvm02 mkdir -p $base_folder/won-client-certs

# create a password file for the certificates, variable ${won_certificate_passwd} must be set from outside the script
# note: name of the password file is fixed in won-docker/image/nginx/nginx-master.conf
echo ${won_certificate_passwd} > won_certificate_passwd_file
ssh root@satvm02 mkdir -p $base_folder/won-server-certs1
scp won_certificate_passwd_file root@satvm02:$base_folder/won-server-certs1/won_certificate_passwd_file
ssh root@satvm02 mkdir -p $base_folder/won-server-certs2
scp won_certificate_passwd_file root@satvm02:$base_folder/won-server-certs2/won_certificate_passwd_file
rm won_certificate_passwd_file

# copy the nginx.conf file to the proxy server
rsync $WORKSPACE/webofneeds/won-docker/image/nginx/nginx-master.conf root@satvm02:$base_folder/nginx-master.conf

echo run docker containers using docker-compose on satvm02
docker -H satvm02:2375 pull webofneeds/bigdata
cd deploy/master_satvm02
docker-compose -H satvm02:2375 down
docker-compose -H satvm02:2375 up --build -d

