#!/bin/sh
export CATALINA_OPTS="${CATALINA_OPTS} -DWON_CONFIG_DIR=/usr/local/tomcat/won/conf -Dlogback.configurationFile=/usr/local/tomcat/won/conf/logback.xml"
echo "added WON params to CATALINA_OPTS"