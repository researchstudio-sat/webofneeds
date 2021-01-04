#!/usr/bin/bash
script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
set -o errexit
set -o errtrace
set -o nounset
source ${script_path}/setenv.sh
if (${setup_ok})
then
  "${script_path}"/setup-volumes.sh
  ## start the containers
  echo "Starting environment based on docker-compose env var COMPOSE_FILE=${COMPOSE_FILE}"
  docker-compose up -d
else
  echo "setup not ok, not starting environment"
fi