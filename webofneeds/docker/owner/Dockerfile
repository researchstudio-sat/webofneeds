FROM tomcat:7.0
RUN apt-get update && apt-get install -y \
    vim \
    less \
	dos2unix
ADD ./webofneeds/won-owner-webapp/target/owner.war /usr/local/tomcat/webapps/
ADD ./webofneeds/conf /usr/local/tomcat/won/conf
ADD ./webofneeds/docker/owner/setenv.sh /usr/local/tomcat/bin/
ADD ./webofneeds/docker/owner/tomcat-users.xml /usr/local/tomcat/conf/
RUN dos2unix /usr/local/tomcat/bin/setenv.sh
