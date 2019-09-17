# before docker build can be executed, the jar file and the conf
# directory have to be copied into this folder (done by maven build)

# use java as a base image
# fix java version here until the following issue is resolved: https://github.com/researchstudio-sat/webofneeds/issues/1229
FROM openjdk:8u121-jdk

# add webofneeds default config env variables
ENV WON_CONFIG_DIR=/usr/src/bots/conf
ENV LOGBACK_CONFIG=logback.xml

# add the default monitoring output directory
RUN mkdir -p /usr/src/bots/monitoring/logs
ENV monitoring.output.dir=/usr/src/bots/monitoring/logs
# ENV MAIN_BOT=won.bot.app.DebugBotApp #IDK WHAT TO DO HERE

# add the jar and the conf directory
ADD ./bots.jar /usr/src/bots/
ADD ./conf ${WON_CONFIG_DIR}

# add certificates directory
RUN mkdir -p /usr/src/bots/client-certs

# start echo bot
WORKDIR /usr/src/bots/
CMD java -DWON_CONFIG_DIR=${WON_CONFIG_DIR}/ \
-Dlogback.configurationFile=${WON_CONFIG_DIR}/${LOGBACK_CONFIG} \
${JMEM_OPTS} \
${JMX_OPTS} \
-cp "bots.jar" ${MAIN_BOT}