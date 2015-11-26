#!/bin/sh
export CATALINA_OPTS="${CATALINA_OPTS} -DWON_CONFIG_DIR=${WON_CONFIG_DIR} -Dlogback.configurationFile=${WON_CONFIG_DIR}/${LOGBACK_CONFIG} -Xmx250m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/temp/"
echo "added WON params to CATALINA_OPTS"
LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/apr/lib
export LD_LIBRARY_PATH
