#!/bin/sh
export CATALINA_OPTS="${CATALINA_OPTS} -DWON_CONFIG_DIR=${WON_CONFIG_DIR} -Dlogback.configurationFile=${WON_CONFIG_DIR}/${LOGBACK_CONFIG}"
echo "added WON params to CATALINA_OPTS"