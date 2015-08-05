# before docker build can be executed, the war file, the tomcat libs folder
# and the conf directory have to be copied into this folder
FROM tomcat:7.0
RUN apt-get update && apt-get install -y \
    vim \
    less \
	dos2unix
ADD ./owner.war /usr/local/tomcat/webapps/
ADD ./tomcat-libs/* /usr/local/tomcat/lib/
ADD ./conf /usr/local/tomcat/won/conf
ADD ./setenv.sh /usr/local/tomcat/bin/
ADD ./tomcat-users.xml /usr/local/tomcat/conf/
RUN dos2unix /usr/local/tomcat/bin/setenv.sh
