#!/usr/bin/bash
source ./setenv.sh
./setup-postgres-volume.sh
## start the containers
docker-compose up -d 