This file explains how to set up a webofneeds application to 
use the config directory. The config directory is the one containing
node.properties, owner.properties, logback.xml, and this README

=STEP 1: point the application to the config dir = 
Either:
set an environment variable named 'WON_CONFIG_DIR' pointing to the config directory.
Or: 
pass a system property value when starting the VM (most probably tomcat) by
adding -DWON_CONFIG_DIR=[your-config-dir]

=STEP 2: configure logging (webapps) =
Either:
set an environment variable named 'logback.configurationFile' pointing to the 
logback.xml file in the config directory
Or:
pass a system property value when starting the VM (most probably tomcat) by
adding the VM parameter -Dlogback.configurationFile=[your-config-dir]/logback.xml

=STEP 3: configure logging (command line runners) =
Either:
set an environment variable named 'logging.config' pointing to the 
logback.xml file in the config directory
Or:
pass a system property value when starting the VM (most probably tomcat) by
adding the VM parameter -Dlogging.config=[your-config-dir]/logback.xml


= HINTS =
== Logging ==
=== Debug the config ===
 If you can't get logging to work properly, try the 'debug="true"' attribute in the 
 logback config file:
 [snip]
 <configuration debug="true" scan="true" scanPeriod="30 seconds">
 [snip]
== Configuring environment variables for tomcat==
 Windows: configure the variables in a file 'setenv.bat' in the [TOMCAT-HOME]\bin folder.
 Unix: configure the variables in a file 'setenv.sh' in the [TOMCAT-HOME]/bin folder.
