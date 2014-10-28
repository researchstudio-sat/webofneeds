This folder contains configuration help for Apache Tomcat 7:

tomcat-jasper-scan-exclude-jars.txt
===================================
contains the name of the jar files that should not be scanned for taglibs etc. during startup.
copy the content and paste it as the value of 

tomcat.util.scan.DefaultJarScanner.jarsToSkip

in catalina.properties, which you find in /etc/tomcat7 or in [TOMCAT-HOME]/conf

