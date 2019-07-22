#!/usr/bin/bash
## edit for your environment here:
export deploy_host=10.0.75.1
export base_folder=/c/tmp/won
cd ${base_folder}
## download the yml file:
curl  https://raw.githubusercontent.com/researchstudio-sat/webofneeds/master/webofneeds/won-docker/deploy/local_image/docker-compose.yml > docker-compose.yml
## start the containers
docker-compose up -d 