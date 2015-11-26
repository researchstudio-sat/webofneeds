# before docker build can be executed, the war file and the conf
# directory have to be copied into this folder (done by maven build)

# use java as a base image
FROM java:8

# add webofneeds default config env variables
ENV WON_CONFIG_DIR=/usr/src/matcher-siren/conf
ENV LOGBACK_CONFIG=logback_info.xml

# add the default monitoring output directory
RUN mkdir -p /usr/src/matcher-siren/monitoring/logs
ENV monitoring.output.dir=/usr/src/matcher-siren/monitoring/logs

# add the jar and the conf directory
ADD ./won-matcher-siren.jar /usr/src/matcher-siren/
ADD ./conf ${WON_CONFIG_DIR}

# start siren matcher
WORKDIR /usr/src/matcher-siren/
CMD java -Dconfig.file=${WON_CONFIG_DIR}/matcher-siren/application.conf \
-DWON_CONFIG_DIR=${WON_CONFIG_DIR}/matcher-siren \
-Dlogback.configurationFile=${WON_CONFIG_DIR}/${LOGBACK_CONFIG} \
${JMEM_OPTS} \
${JMX_OPTS} \
-jar won-matcher-siren.jar