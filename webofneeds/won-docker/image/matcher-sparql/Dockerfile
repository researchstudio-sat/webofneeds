# before docker build can be executed, the war file and the conf
# directory have to be copied into this folder (done by maven build)

# use java as a base image
# fix java version here until the following issue is resolved: https://github.com/researchstudio-sat/webofneeds/issues/1229
FROM openjdk:8u121-jdk

# add webofneeds default config env variables
ENV WON_CONFIG_DIR=/usr/src/matcher-sparql/conf
ENV LOGBACK_CONFIG=logback.xml

# add the default monitoring output directory
RUN mkdir -p /usr/src/matcher-sparql/monitoring/logs
ENV monitoring.output.dir=/usr/src/matcher-sparql/monitoring/logs

# add the jar and the conf directory
ADD ./won-matcher-sparql.jar /usr/src/matcher-sparql/
ADD ./conf ${WON_CONFIG_DIR}

# start solr matcher
WORKDIR /usr/src/matcher-sparql/
CMD java -Dconfig.file=${WON_CONFIG_DIR}/matcher-sparql/application.conf \
-DWON_CONFIG_DIR=${WON_CONFIG_DIR}/matcher-sparql \
-Dlogback.configurationFile=${WON_CONFIG_DIR}/${LOGBACK_CONFIG} \
${JMEM_OPTS} \
${JMX_OPTS} \
-jar won-matcher-sparql.jar