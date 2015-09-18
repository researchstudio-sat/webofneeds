# build the won docker images on every server of the cluster so that everywhere is the latest version available
echo start docker build of images:

# wonnode
docker -H satsrv04:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv05:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv06:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv07:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/

# owner
docker -H satsrv04:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv05:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv06:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv07:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/

# matcher service
docker -H satsrv04:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv05:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv06:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv07:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/

# siren matcher
docker -H satsrv04:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv05:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv06:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv07:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/

# start the won containers on dedicated servers of the cluster
echo run docker containers:

# wonnode 1
docker -H satsrv04:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv04:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv04:2375 run --name=wonnode_int -d -e "uri.host=satsrv04.researchstudio.at" \
-e "http.port=8889" -e "activemq.broker.port=61617" -p 8889:8080 -p 61617:61617 webofneeds/wonnode:int

# wonnode 2
docker -H satsrv05:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv05:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv05:2375 run --name=wonnode_int -d -e "uri.host=satsrv05.researchstudio.at" \
-e "http.port=8889" -e "activemq.broker.port=61617" -p 8889:8080 -p 61617:61617 webofneeds/wonnode:int

sleep 20

# owner 1
docker -H satsrv04:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv04:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
docker -H satsrv04:2375 run --name=owner_int -d -e "node.default.host=satsrv04.researchstudio.at" \
-e "node.default.http.port=8889" -p 8082:8080 webofneeds/owner:int

# owner 2
docker -H satsrv05:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv05:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
docker -H satsrv05:2375 run --name=owner_int -d -e "node.default.host=satsrv05.researchstudio.at" \
-e "node.default.http.port=8889" -p 8082:8080 webofneeds/owner:int

# bigdata
docker -H satsrv06:2375 stop bigdata_int || echo 'No docker container found to stop with name: bigdata_int'
docker -H satsrv06:2375 rm bigdata_int || echo 'No docker container found to remove with name: bigdata_int'
docker -H satsrv06:2375 run --name=bigdata_int -d -p 10000:9999 webofneeds/bigdata

# matcher service
docker -H satsrv06:2375 stop matcher_service_int || echo 'No docker container found to stop with name: matcher_service_int'
docker -H satsrv06:2375 rm matcher_service_int || echo 'No docker container found to remove with name: matcher_service_int'
docker -H satsrv06:2375 run --name=matcher_service_int -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" \
-e "uri.sparql.endpoint=http://satsrv06.researchstudio.at:10000/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=http://satsrv04.researchstudio.at:8889/won/resource,http://satsrv05.researchstudio.at:8889/won/resource" \
-e "cluster.local.port=2561" -e "cluster.seed.port=2561" -p 2561:2561 webofneeds/matcher_service:int

# siren matcher
docker -H satsrv05:2375 stop matcher_siren_int || echo 'No docker container found to stop with name: matcher_siren_int'
docker -H satsrv05:2375 rm matcher_siren_int || echo 'No docker container found to remove with name: matcher_siren_int'
docker -H satsrv05:2375 run --name=matcher_siren_int -d -e "node.host=satsrv05.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" -e "cluster.seed.port=2561" -e "cluster.local.port=2562" \
-p 2562:2562 webofneeds/matcher_siren:int

# siren solr server
docker -H satsrv05:2375 stop sirensolr_int || echo 'No docker container found to stop with name: sirensolr_int'
docker -H satsrv05:2375 rm sirensolr_int || echo 'No docker container found to remove with name: sirensolr_int'
docker -H satsrv05:2375 run --name=sirensolr_int -d -p 8984:8983 webofneeds/sirensolr