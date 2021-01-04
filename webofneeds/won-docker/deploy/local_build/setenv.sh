## edit for your environment here:
script_name=${BASH_SOURCE[0]}
script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export setup_ok=false

#set -o errexit
set -o errtrace
set -o nounset

if [[ -v WON_GIT_REPO ]]
then
  echo "WON_GIT_REPO: ${WON_GIT_REPO}"
else
  WON_GIT_REPO="${script_path}/../../../.."
  WON_GIT_REPO="$( cd "${WON_GIT_REPO}" >/dev/null 2>&1 && pwd )"
  echo "WON_GIT_REPO not set, trying default: ${WON_GIT_REPO}"
fi

if [[ -d "${WON_GIT_REPO}/webofneeds" && -d "${WON_GIT_REPO}/scripts" ]]
then
  export WON_GIT_REPO=${WON_GIT_REPO}
  echo "Success: won git repository located"
else
  echo "Error: unable to locate won git repository. Set WON_GIT_REPO variable to point to the directory which webofneeds was cloned into" >&2
  unset WON_GIT_REPO
fi
export base_folder=$(pwd)
export COMPOSE_FILE=${script_path}/docker-compose.yml
echo "Setting COMPOSE_FILE to ${COMPOSE_FILE}"
export setup_ok=true



