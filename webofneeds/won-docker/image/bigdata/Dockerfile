FROM nawer/blazegraph:2.1.4

ADD ./RWStore.properties /docker-entrypoint-initdb.d/kb/RWStore.properties
RUN ["mkdir", "-p", "/docker-entrypoint-initdb.d/kb/data"]