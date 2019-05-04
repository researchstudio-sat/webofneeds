# before docker build can be executed, the war file, the tomcat libs folder
# and the conf directory have to be copied into this folder (done by maven build)
FROM tomcat:9.0.16-jre8
RUN apt-get update && apt-get install -y \
    vim \
    less \
	dos2unix

# add webofneeds default config env variables
ENV WON_CONFIG_DIR=/usr/local/tomcat/won/conf
ENV LOGBACK_CONFIG=logback.xml
ENV CERTIFICATE_PASSWORD=changeit

# configure tomcat
ADD ./setenv.sh /usr/local/tomcat/bin/
ADD ./ssl/server.xml /usr/local/tomcat/conf/

# uncomment the following line to access tomcat manager with admin user
#ADD ./tomcat-users.xml /usr/local/tomcat/conf/

# remove the applications not needed => comment the next two lines for having access to the tomcat manager
RUN rm -rf /usr/local/tomcat/webapps/*
RUN rm -rf /usr/local/tomcat/work/Catalina/localhost/*

# add and extract the application war files to the webapps folder
ADD ./owner.war /usr/local/tomcat/webapps/
RUN unzip /usr/local/tomcat/webapps/owner.war -d /usr/local/tomcat/webapps/owner
RUN rm /usr/local/tomcat/webapps/owner.war

# add the bouncy castle libraries to the jre as well as to the java.security config (we need them in the jre, only tomcat lib folder doesn't work)
ADD ./required-libs/* /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/
ADD ./jce/* /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/

# add the webofneeds configuration files for the application
ADD ./conf/owner.properties /usr/local/tomcat/won/conf/owner.properties
ADD ./conf/logback* /usr/local/tomcat/won/conf/

# prepare folder for server certificates - this path should be the same as the folder containing
# SSLCertificateFile/Key configured in server.xml:
RUN mkdir -p /usr/local/tomcat/conf/ssl/

# prepare folder for client certificates - this path should be the same as configured for client key/trust stores in
# application's corresponding conf/*.properties file
RUN mkdir -p /usr/local/tomcat/won/client-certs/

# convert Windows/Mac text file to Linux text file
RUN dos2unix /usr/local/tomcat/bin/setenv.sh
