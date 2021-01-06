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

# switch classloader hacks on if in dev mode (enabled by providing DEV_MODE=true in the build
# environment or via --build-arg DEV_MODE=true
ARG DEV_MODE=false


# uncomment the following line to access tomcat manager with admin user
#ADD ./tomcat-users.xml /usr/local/tomcat/conf/

# remove the applications not needed => comment the next two lines for having access to the tomcat manager
RUN rm -rf /usr/local/tomcat/webapps/*
RUN rm -rf /usr/local/tomcat/work/Catalina/localhost/*

# add and extract the application war files to the webapps folder
ADD ./won.war /usr/local/tomcat/webapps/
RUN unzip /usr/local/tomcat/webapps/won.war -d /usr/local/tomcat/webapps/won
RUN rm /usr/local/tomcat/webapps/won.war

## if DEV_MODE=true, remove all won dependency jars (we mount the respective class files into the container)
RUN ${DEV_MODE} \
&& rm /usr/local/tomcat/webapps/won/WEB-INF/lib/won-auth*.jar \
                      /usr/local/tomcat/webapps/won/WEB-INF/lib/won-utils-shacl2java*.jar \
                      /usr/local/tomcat/webapps/won/WEB-INF/lib/won-core*.jar \
                      /usr/local/tomcat/webapps/won/WEB-INF/lib/won-vocab*.jar \
                      /usr/local/tomcat/webapps/won/WEB-INF/lib/won-cryptography*.jar \
                      /usr/local/tomcat/webapps/won/WEB-INF/lib/won-node*.jar \
|| echo " ---| Step skipped (DEV_MODE=${DEV_MODE})"

#RUN ${DEV_MODE} \
#&& mkdir -p /usr/local/tomcat/won/libs \
#&& mv /usr/local/tomcat/webapps/won/WEB-INF/lib/* /usr/local/tomcat/won/libs \
#|| echo " ---| Step skipped (DEV_MODE=${DEV_MODE})"

# add the bouncy castle libraries to the jre as well as to the java.security config (we need them in the jre, only tomcat lib folder doesn't work)
ADD ./required-libs/* /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/
ADD ./jce/* /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/

# add the webofneeds configuration files for the application
ADD ./conf/node.properties /usr/local/tomcat/won/conf/node.properties
ADD ./conf/logback* /usr/local/tomcat/won/conf/

# prepare folder for server certificates - this path should be the same as the folder containing
# SSLCertificateFile/Key configured in server.xml:
RUN mkdir -p /usr/local/tomcat/conf/ssl/

# prepare folder for client certificates - this path should be the same as configured for client key/trust stores in
# application's corresponding conf/*.properties file
RUN mkdir -p /usr/local/tomcat/won/client-certs/

# convert Windows/Mac text file to Linux text file
RUN dos2unix /usr/local/tomcat/bin/setenv.sh
