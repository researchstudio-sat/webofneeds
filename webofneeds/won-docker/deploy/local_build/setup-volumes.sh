#!/usr/bin/env bash

# postgres has trouble to just use normal volume mount (must be created by same user as
# the one executing postgres)
# this is the workaround from
# https://forums.docker.com/t/trying-to-get-postgres-to-work-on-persistent-windows-mount-two-issues/12456/5?u=friism
# (see also 'volumes' section in docker-compose.yml)
echo "creating posgres-data volume"
docker volume create --name postgres-data -d local
echo "creating mongodb-data volume"
docker volume create --name mongodb-data -d local
echo "creating wonnode-won-deps volume"
docker volume create --name wonnode-won-deps -d local
echo "creating owner-won-deps volume"
docker volume create --name owner-won-deps -d local
echo -e "listing volumes:\n\n"

docker volume ls