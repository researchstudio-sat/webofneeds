#!/usr/bin/env bash

# postgres has trouble to just use normal volume mount (must be created by same user as
# the one executing postgres)
# this is the workaround from
# https://forums.docker.com/t/trying-to-get-postgres-to-work-on-persistent-windows-mount-two-issues/12456/5?u=friism
# (see also 'volumes' section in docker-compose.yml)
echo "creating posgres volume"
docker volume create --name postgres-data -d local
echo -e "listing volumes:\n\n"
docker volume ls