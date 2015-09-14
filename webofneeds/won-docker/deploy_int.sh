# build won docker images and deploy to sat cluster
echo Start Docker build and deployment:

# wonnode
docker -H satsrv04:2375 build -t webofneeds/wonnode:int $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv04:2375 stop wonnode_int || echo 'No docker container found to stop with name: wonnode_int'
docker -H satsrv04:2375 rm wonnode_int || echo 'No docker container found to remove with name: wonnode_int'
docker -H satsrv04:2375 run --name=wonnode_int -d -e "uri.host=satsrv04.researchstudio.at" -e "http.port=8889" -e "activemq.broker.port=61617" -p 8889:8080 -p 61617:61617 webofneeds/wonnode:int

# owner
docker -H satsrv04:2375 build -t webofneeds/owner:int $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv04:2375 stop owner_int || echo 'No docker container found to stop with name: owner_int'
docker -H satsrv04:2375 rm owner_int || echo 'No docker container found to remove with name: owner_int'
sleep 20
docker -H satsrv04:2375 run --name=owner_int -d -e "node.default.host=satsrv04.researchstudio.at" -e "node.default.http.port=8889" -p 8082:8080 webofneeds/owner:int

# bigdata
docker -H satsrv06:2375 stop bigdata_int || echo 'No docker container found to stop with name: bigdata_int'
docker -H satsrv06:2375 rm bigdata_int || echo 'No docker container found to remove with name: bigdata_int'
docker -H satsrv06:2375 run --name=bigdata_int -d -p 9999:9999 webofneeds/bigdata

# matcher service
docker -H satsrv06:2375 build -t webofneeds/matcher_service:int $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv06:2375 stop matcher_service_int || echo 'No docker container found to stop with name: matcher_service_int'
docker -H satsrv06:2375 rm matcher_service_int || echo 'No docker container found to remove with name: matcher_service_int'
docker -H satsrv06:2375 run --name=matcher_service_int -d -e "node.host=satsrv06.researchstudio.at" -e "cluster.seed.host=satsrv06.researchstudio.at" -e "uri.sparql.endpoint=http://satsrv06.researchstudio.at:9999/bigdata/namespace/kb/sparql" -e "wonNodeController.wonNode.crawl=http://satsrv04.researchstudio.at:8889/won/resource" -p 2551:2551 webofneeds/matcher_service:int

# siren matcher
docker -H satsrv05:2375 build -t webofneeds/matcher_siren:int $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv05:2375 stop matcher_siren_int || echo 'No docker container found to stop with name: matcher_siren_int'
docker -H satsrv05:2375 rm matcher_siren_int || echo 'No docker container found to remove with name: matcher_siren_int'
docker -H satsrv05:2375 run --name=matcher_siren_int -d -e "node.host=satsrv05.researchstudio.at" -e "cluster.seed.host=satsrv06.researchstudio.at" -p 2552:2552 webofneeds/matcher_siren:int

# siren solr server
docker -H satsrv05:2375 stop sirensolr_int || echo 'No docker container found to stop with name: sirensolr_int'
docker -H satsrv05:2375 rm sirensolr_int || echo 'No docker container found to remove with name: sirensolr_int'
docker -H satsrv05:2375 run --name=sirensolr_int -d -p 8983:8983 webofneeds/sirensolr