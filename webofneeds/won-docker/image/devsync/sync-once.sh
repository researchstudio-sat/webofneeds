#!/bin/sh

cwd=$(pwd)
src=$1
dest=$2

rsync -vra --relative "${src}/./" \
           ${dest}/ && \
       echo "done syncing ${src}/ to ${dest}/" >&2