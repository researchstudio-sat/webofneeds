#!/usr/bin/env bash
set -o errexit
set -o errtrace
set -o nounset

srcDir=/srcData
destDir=/destData
echo ""
echo "**************************************"
echo "* syncs the contents of all folders "
echo "* in ${srcDir} to ${destDir} "
echo "**************************************"

## uncomment to set up some debugging data
# mkdir -p ${srcDir}/a
# mkdir -p ${srcDir}/b
# mkdir -p ${srcDir}/c
# touch ${srcDir}/a/x
# echo "bar" > ${srcDir}/b/foo.txt
# mkdir -p ${destDir}
# echo "bar" > ${destDir}/bar.txt

if [[ "${DAEMON_MODE}" == "true" ]]
then
  DAEMON_MODE=true
  echo "Running in daemon mode - will sync as soon as changes are made and run forever"
else
  DAEMON_MODE=false
  echo "Not running in daemon mode - will sync everything once, then exit"
fi

if [[ "${DELETE_CONTENTS_ON_STARTUP}" == "true" ]]
then
  echo "Deleting contents of destination folder as DELETE_CONTENTS_ON_STARTUP=${DELETE_CONTENTS_ON_STARTUP}"
  rm -rf ${destDir}/*
else
  echo "Not deleting contents of destination folder as DELETE_CONTENTS_ON_STARTUP=${DELETE_CONTENTS_ON_STARTUP}"
fi


syncsFound=false
echo "$(date): Checking for folders to sync in ${srcDir}"
for dir in $(ls ${srcDir})
do
  src=${srcDir}/${dir}
  echo "- - - - - - - - - -"
  echo "Found ${src} ..."
  if [[ -d ${src} ]]
  then
    echo -e "\e[92mSyncing ${src}\e[0m to $destDir/"
    syncsFound=true
    /sync/sync-once.sh ${src} ${destDir}
    if (${DAEMON_MODE})
    then
      echo -e "\e[92mSetting up continuous syncing of ${src}\e[0m to $destDir/"
      /sync/watch-and-sync.sh ${src} ${destDir} &
    fi
  fi
done

echo "-------------------"
if (! ${DAEMON_MODE})
then
  if (${syncsFound})
  then
    echo "$(date): Done syncing"
  else
    echo "$(date): Nothing found to sync"
  fi
  exit
fi

if (${syncsFound})
then
  echo "$(date): Finished setup"
  echo "Continous syncing in progress..."
else
  echo "$(date): No folders found in ${srcDir}, exiting..."
  exit
fi

# wait forever if syncsFound
while (${syncsFound})
do
  echo "$(date): Continous syncing in progress. Next message in 1 hour"
  sleep 3600
done
