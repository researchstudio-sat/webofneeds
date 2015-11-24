# fail the whole script if one command fails
set -e

# build won docker images and deploy to sat cluster
echo start docker build and deployment:

# postgres db 1
docker -H satsrv04:2375 pull webofneeds/postgres
docker -H satsrv04:2375 stop postgres_dev  || echo 'No docker container found to stop with name: postgres_dev'
docker -H satsrv04:2375 rm postgres_dev || echo 'No docker container found to remove with name: postgres_dev'
docker -H satsrv04:2375 run --name=postgres_dev -d -p 5432:5432 webofneeds/postgres

# postgres db 2
docker -H satsrv05:2375 stop postgres_dev || echo 'No docker container found to stop with name: postgres_dev'
docker -H satsrv05:2375 rm postgres_dev || echo 'No docker container found to remove with name: postgres_dev'
docker -H satsrv05:2375 run --name=postgres_dev -d -p 5432:5432 webofneeds/postgres

sleep 10

# wonnode 1
docker -H satsrv04:2375 build -t webofneeds/wonnode:dev $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv04:2375 stop wonnode_dev || echo 'No docker container found to stop with name: wonnode_dev'
docker -H satsrv04:2375 rm wonnode_dev || echo 'No docker container found to remove with name: wonnode_dev'
docker -H satsrv04:2375 run --name=wonnode_dev -d -e "uri.host=satsrv04.researchstudio.at" \
-e "http.port=8888" -e "activemq.broker.port=61616" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5432/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v /home/install/won-certs:/usr/local/tomcat/conf/ssl/ \
-p 8888:8443 -p 61616:61616 webofneeds/wonnode:dev


# wonnode 2
docker -H satsrv05:2375 build -t webofneeds/wonnode:dev $WORKSPACE/webofneeds/won-docker/wonnode/
docker -H satsrv05:2375 stop wonnode_dev || echo 'No docker container found to stop with name: wonnode_dev'
docker -H satsrv05:2375 rm wonnode_dev || echo 'No docker container found to remove with name: wonnode_dev'
docker -H satsrv05:2375 run --name=wonnode_dev -d -e "uri.host=satsrv05.researchstudio.at" \
-e "http.port=8888" -e "activemq.broker.port=61616" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5432/won_node" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v /home/install/won-certs:/usr/local/tomcat/conf/ssl/ \
-p 8888:8443 -p 61616:61616 webofneeds/wonnode:dev

sleep 20

# owner 1
docker -H satsrv04:2375 build -t webofneeds/owner:dev $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv04:2375 stop owner_dev || echo 'No docker container found to stop with name: owner_dev'
docker -H satsrv04:2375 rm owner_dev || echo 'No docker container found to remove with name: owner_dev'
docker -H satsrv04:2375 run --name=owner_dev -d -e "node.default.host=satsrv04.researchstudio.at" \
-e "node.default.http.port=8888" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv04:5432/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v /home/install/won-certs:/usr/local/tomcat/conf/ssl/ \
-p 8081:8443 webofneeds/owner:dev

# owner 2
docker -H satsrv05:2375 build -t webofneeds/owner:dev $WORKSPACE/webofneeds/won-docker/owner/
docker -H satsrv05:2375 stop owner_dev || echo 'No docker container found to stop with name: owner_dev'
docker -H satsrv05:2375 rm owner_dev || echo 'No docker container found to remove with name: owner_dev'
docker -H satsrv05:2375 run --name=owner_dev -d -e "node.default.host=satsrv05.researchstudio.at" \
-e "node.default.http.port=8888" \
-e "db.sql.jdbcDriverClass=org.postgresql.Driver" \
-e "db.sql.jdbcUrl=jdbc:postgresql://satsrv05:5432/won_owner" \
-e "db.sql.user=won" -e "db.sql.password=won" \
-v /home/install/won-certs:/usr/local/tomcat/conf/ssl/ \
-p 8081:8443 webofneeds/owner:dev

# bigdata
docker -H satsrv06:2375 pull webofneeds/bigdata
docker -H satsrv06:2375 stop bigdata_dev || echo 'No docker container found to stop with name: bigdata_dev'
docker -H satsrv06:2375 rm bigdata_dev || echo 'No docker container found to remove with name: bigdata_dev'
docker -H satsrv06:2375 run --name=bigdata_dev -d -p 9999:9999 webofneeds/bigdata

# matcher service
docker -H satsrv06:2375 build -t webofneeds/matcher_service:dev $WORKSPACE/webofneeds/won-docker/matcher-service/
docker -H satsrv06:2375 stop matcher_service_dev || echo 'No docker container found to stop with name: matcher_service_dev'
docker -H satsrv06:2375 rm matcher_service_dev || echo 'No docker container found to remove with name: matcher_service_dev'
docker -H satsrv06:2375 run --name=matcher_service_dev -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" \
-e "uri.sparql.endpoint=http://satsrv06.researchstudio.at:9999/bigdata/namespace/kb/sparql" \
-e "wonNodeController.wonNode.crawl=https://satsrv04.researchstudio.at:8888/won/resource,https://satsrv05.researchstudio.at:8888/won/resource" \
-e "cluster.local.port=2551" -e "cluster.seed.port=2551" -p 2551:2551 webofneeds/matcher_service:dev

# siren solr server
docker -H satsrv06:2375 pull webofneeds/sirensolr
docker -H satsrv06:2375 stop sirensolr_dev || echo 'No docker container found to stop with name: sirensolr_dev'
docker -H satsrv06:2375 rm sirensolr_dev || echo 'No docker container found to remove with name: sirensolr_dev'
docker -H satsrv06:2375 run --name=sirensolr_dev -d -p 7070:8080 -p 8983:8983 webofneeds/sirensolr

sleep 10

# siren matcher
docker -H satsrv06:2375 build -t webofneeds/matcher_siren:dev $WORKSPACE/webofneeds/won-docker/matcher-siren/
docker -H satsrv06:2375 stop matcher_siren_dev || echo 'No docker container found to stop with name: matcher_siren_dev'
docker -H satsrv06:2375 rm matcher_siren_dev || echo 'No docker container found to remove with name: matcher_siren_dev'
docker -H satsrv06:2375 run --name=matcher_siren_dev -d -e "node.host=satsrv06.researchstudio.at" \
-e "cluster.seed.host=satsrv06.researchstudio.at" -e "cluster.seed.port=2551" -e "cluster.local.port=2552" \
-e "matcher.siren.uri.solr.server=http://satsrv06.researchstudio.at:8983/solr/won/" \
-e "matcher.siren.uri.solr.server.public=http://satsrv06.researchstudio.at:8983/solr/#/won/" \
-p 2552:2552 webofneeds/matcher_siren:dev

# push the newly build images to the dockerhub
docker -H localhost:2375 login -u heikofriedrich
docker -H localhost:2375 push webofneeds/wonnode:dev
docker -H localhost:2375 push webofneeds/owner:dev
docker -H localhost:2375 push webofneeds/matcher_service:dev
docker -H localhost:2375 push webofneeds/matcher_siren:dev