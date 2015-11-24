# fail the whole script if one command fails
set -e

# build the won docker images on every server of the cluster so that everywhere is the latest version available
echo start docker build of images:

# wonnode 1
docker -H satcluster01:2375 build -t webofneeds/wonnode:master $WORKSPACE/webofneeds/won-docker/wonnode/

# owner 1
docker -H satcluster01:2375 build -t webofneeds/owner:master $WORKSPACE/webofneeds/won-docker/owner/

# matcher service
docker -H satcluster01:2375 build -t webofneeds/matcher_service:master $WORKSPACE/webofneeds/won-docker/matcher-service/

# siren matcher
docker -H satcluster01:2375 build -t webofneeds/matcher_siren:master $WORKSPACE/webofneeds/won-docker/matcher-siren/

# wonnode/owner server certificate generator
docker -H satcluster01:2375 build -t webofneeds/gencert:master $WORKSPACE/webofneeds/won-docker/gencert/


# start the won containers on dedicated servers of the cluster
echo run docker containers:

# postgres db
docker -H satcluster01:2375 pull webofneeds/postgres
docker -H satcluster01:2375 stop postgres_ma || echo 'No docker container found to stop with name: postgres_ma'
docker -H satcluster01:2375 rm postgres_ma || echo 'No docker container found to remove with name: postgres_ma'
docker -H satcluster01:2375 run --name=postgres_ma -d -p 5433:5432 webofneeds/postgres


sleep 10

# wonnode/owner server certificate generator
# PLEASE NOTE that value of PASS should be the same used in your server.xml for SSLPassword on wonnode and owner,
# and the same as activemq.broker.keystore.password used in your wonnode activemq spring configurations for broker
docker -H satcluster01:2375 rm gencert_ma || echo 'No docker container found to remove with name: gencert_ma'
docker -H satcluster01:2375 run --name=gencert_ma -e CN="satcluster01.researchstudio.at" -e "PASS=changeit" \
-v /home/install/won-server-certs:/usr/local/certs/out/  webofneeds/gencert:master


sleep 5


# wonnode
docker -H satcluster01:2375 stop wonnode_ma || echo 'No docker container found to stop with name: wonnode_ma'
docker -H satcluster01:2375 rm wonnode_ma || echo 'No docker container found to remove with name: wonnode_ma'
docker -H satcluster01:2375 run --name=wonnode_ma -d -e "uri.host=satcluster01.researchstudio.at" -e "http.port=8889" -e \
"activemq.broker.port=61617" -p 8889:8443 -p 61617:61617 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/wonnode_ma:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satcluster01:5433/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
webofneeds/wonnode:master


# owner
docker -H satcluster01:2375 stop owner_ma || echo 'No docker container found to stop with name: owner_ma'
docker -H satcluster01:2375 rm owner_ma || echo 'No docker container found to remove with name: owner_ma'
docker -H satcluster01:2375 run --name=owner_ma -d -e "node.default.host=satcluster01.researchstudio.at" \
-e "node.default.http.port=8889" -p 8082:8443 \
-v /home/install/won-server-certs:/usr/local/tomcat/conf/ssl/ \
-v /home/install/won-client-certs/owner_ma:/usr/local/tomcat/won/client-certs/ \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satcluster01:5433/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
webofneeds/owner:master


# bigdata
docker -H satcluster01:2375 pull webofneeds/bigdata
docker -H satcluster01:2375 stop bigdata_ma || echo 'No docker container found to stop with name: bigdata_ma'
docker -H satcluster01:2375 rm bigdata_ma || echo 'No docker container found to remove with name: bigdata_ma'
docker -H satcluster01:2375 run --name=bigdata_ma -d -p 10000:9999 webofneeds/bigdata

# matcher service
docker -H satcluster01:2375 stop matcher_service_ma || echo 'No docker container found to stop with name: matcher_service_ma'
docker -H satcluster01:2375 rm matcher_service_ma || echo 'No docker container found to remove with name: matcher_service_ma'
docker -H satcluster01:2375 run --name=matcher_service_ma -d -e "node.host=satcluster01.researchstudio.at" \
-e "cluster.seed.host=satcluster01.researchstudio.at" \
-e "uri.sparql.endpoint=http://satcluster01.researchstudio.at:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://satcluster01.researchstudio.at:8889/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -p 2561:2561 \
-v /home/install/won-client-certs/matcher_service_ma:/usr/src/matcher-service/client-certs/ \
webofneeds/matcher_service:master

# siren solr server
docker -H satcluster01:2375 pull webofneeds/sirensolr
docker -H satcluster01:2375 stop sirensolr_ma || echo 'No docker container found to stop with name: sirensolr_ma'
docker -H satcluster01:2375 rm sirensolr_ma || echo 'No docker container found to remove with name: sirensolr_ma'
docker -H satcluster01:2375 run --name=sirensolr_ma -d -p 7071:8080 -p 8984:8983 webofneeds/sirensolr

sleep 10

# siren matcher
docker -H satcluster01:2375 stop matcher_siren_ma || echo 'No docker container found to stop with name: matcher_siren_ma'
docker -H satcluster01:2375 rm matcher_siren_ma || echo 'No docker container found to remove with name: matcher_siren_ma'
docker -H satcluster01:2375 run --name=matcher_siren_ma -d -e "node.host=satcluster01.researchstudio.at" \
-e "cluster.seed.host=satcluster01.researchstudio.at" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-e "matcher.siren.uri.solr.server=http://satcluster01.researchstudio.at:8984/solr/won/" \
-e "matcher.siren.uri.solr.server.public=http://satcluster01.researchstudio.at:8984/solr/#/won/" \
-p 2562:2562 webofneeds/matcher_siren:master


# if everything works up to this point - build :master images locally and push these local images into the dockerhub:
# build:
docker -H localhost:2375 build -t webofneeds/gencert:master $WORKSPACE/webofneeds/won-docker/gencert/
docker -H localhost:2375 build -t webofneeds/wonnode:master $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H localhost:2375 build -t webofneeds/owner:master $WORKSPACE/webofneeds/won-docker/owner/
docker -H localhost:2375 build -t webofneeds/matcher_service:master $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H localhost:2375 build -t webofneeds/matcher_siren:master $WORKSPACE/webofneeds/won-docker/matcher-siren/
# push:
docker -H localhost:2375 login -u heikofriedrich
docker -H localhost:2375 push webofneeds/gencert:master
docker -H localhost:2375 push webofneeds/wonnode:master
docker -H localhost:2375 push webofneeds/owner:master
docker -H localhost:2375 push webofneeds/matcher_service:master
docker -H localhost:2375 push webofneeds/matcher_siren:master
