#!/bin/sh

# Creates an archive for uploading to Amazon Device Farm
# Usage: deploy.sh [zip_file]

set -e

SCRIPT=`readlink -f "$0"`

DIR=`dirname $SCRIPT`

DEST="${1:-$DIR/test_bundle.zip}"

. "$DIR/bin/activate"

rm -f "$DEST"

pip wheel --wheel-dir "$DIR/wheelhouse" -r "$DIR/requirements.txt"

zip -r "$DEST" $DIR/tests/test_*.py "$DIR/wheelhouse/" "$DIR/requirements.txt"