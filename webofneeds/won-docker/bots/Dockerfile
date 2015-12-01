# This image is build and pushed to the dockerhub manually (webofneeds/bigdata)
# The bigdata-1.5.2-bundled.jar file has to be copied manually to this folder

# use java as a base image
FROM java:8

# download the big-data bundled jar
ADD ./bigdata-1.5.2-bundled.jar /usr/src/bigdata/bigdata-bundled.jar

# start bigdata on port 9999
WORKDIR /usr/src/bigdata/
CMD java -server -Xmx4g -jar bigdata-bundled.jar

---------
# before docker build can be executed, the jar file and the conf
# directory have to be copied into this folder (done by maven build)

# use java as a base image
FROM java:8

# add webofneeds default config env variables
ENV WON_CONFIG_DIR=/usr/src/bots/conf
ENV LOGBACK_CONFIG=logback_info.xml

# add the default monitoring output directory
RUN mkdir -p /usr/src/bots/monitoring/logs
ENV monitoring.output.dir=/usr/src/bots/monitoring/logs
ENV MAIN_BOT=won.bot.app.EchoBotApp

# add the jar and the conf directory
ADD ./bots.jar /usr/src/bots/
ADD ./conf ${WON_CONFIG_DIR}

# start echo bot
WORKDIR /usr/src/bots/
CMD java -DWON_CONFIG_DIR=${WON_CONFIG_DIR}/ \
-Dlogback.configurationFile=${WON_CONFIG_DIR}/${LOGBACK_CONFIG} \
${JMEM_OPTS} \
${JMX_OPTS} \
-cp "bots.jar" ${MAIN_BOT}