FROM tomcat:7.0
RUN apt-get update && apt-get install -y \
    vim \
    less \
	dos2unix
ADD ./won.war /usr/local/tomcat/webapps/
ADD ./conf /usr/local/tomcat/won/conf
ADD ./setenv.sh /usr/local/tomcat/bin/
ADD ./tomcat-users.xml /usr/local/tomcat/conf/
RUN dos2unix /usr/local/tomcat/bin/setenv.sh
