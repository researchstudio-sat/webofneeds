# This image is build and pushed to the dockerhub manually (webofneeds/sirensolr)
# The siren-solr-1.4-bin zip file has to be copied and extracted manually to this folder
# The solr server should be accessible at port 8983

# use java as a base image
FROM tomcat:8

# add the siren/solr files needed in tomcat
ADD ./siren-solr-1.4-bin/example/webapps/solr.war /usr/local/tomcat/webapps/solr.war
ADD ./siren-solr-1.4-bin/example/solr /usr/local/tomcat/solr
ADD ./siren-solr-1.4-bin/example/webapps/solr.war /usr/local/tomcat/solr/solr.war
ADD ./siren-solr-1.4-bin/example/resources /usr/local/tomcat/lib
ADD ./siren-solr-1.4-bin/example/lib/ext /usr/local/tomcat/lib

# overwrite the server.xml of tomcat (increased maxHttpHeaderSize in http connector)
ADD ./server.xml /usr/local/tomcat/conf/server.xml

# add the following log level specifications to prevent solr from logging every query
ADD ./log4j.properties /usr/local/tomcat/lib/log4j.properties

# add the won core to the siren/solr instance
ADD ./core/won /usr/local/tomcat/solr/won
