FROM alpine:3.12.3

RUN apk add --no-cache --virtual .run-deps rsync bash dos2unix inotify-tools && rm -rf /var/cache/apk/*

RUN mkdir -p /srcData && mkdir -p /destData && mkdir -p /sync

ADD ./sync.sh /sync/
ADD ./watch-and-sync.sh /sync/
ADD ./sync-once.sh /sync/

RUN chmod +x /sync/*.sh

RUN dos2unix /sync/*.sh

CMD ["/sync/sync.sh"]
