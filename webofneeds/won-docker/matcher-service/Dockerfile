# before docker build can be executed, the war file, the tomcat and the
# conf directory have to be copied into this folder (done by maven build)

# use java as a base image
FROM java:8

# add webofneeds default config env variables
ENV WON_CONFIG_DIR=/usr/src/matcher-service/conf
ENV LOGBACK_CONFIG=logback_info.xml

# add the jar and the conf directory
ADD ./won-matcher-service.jar /usr/src/matcher-service/
ADD ./conf ${WON_CONFIG_DIR}

# start matcher service
WORKDIR /usr/src/matcher-service/
CMD java -Dconfig.file=${WON_CONFIG_DIR}/matcher-service/application.conf -DWON_CONFIG_DIR=${WON_CONFIG_DIR}/matcher-service/ -Dlogback.configurationFile=${WON_CONFIG_DIR}/${LOGBACK_CONFIG} -jar won-matcher-service.jar