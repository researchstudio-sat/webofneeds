#!/bin/sh

cwd=$(pwd)
src=$1
dest=$2

inotifywait -mr \
  --timefmt '%d/%m/%y %H:%M' --format '%T %w %f' ${src}/ |
while read -r date time dir file; do
       changed_abs=${dir}${file}
       # inserting the '/./' causes rsync to preserve the
       # dir structure starting after '/./'
       changed_rel="${src}/./${changed_abs#"${src}"/}"
       rsync -vra --relative "$changed_rel" \
           ${dest}/ && \
       echo "At ${time} on ${date}, file $changed_abs was copied to ${dest}/" >&2
done