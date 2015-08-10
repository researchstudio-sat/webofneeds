# before docker build can be executed, the war file, the tomcat libs folder
# and the conf directory have to be copied into this folder
FROM tomcat:8.0-jre8
RUN apt-get update && apt-get install -y \
    vim \
    less \
	dos2unix
# upgrade to the newest jre8
RUN apt-get update && apt-get upgrade openjdk-8-jre -y
ADD ./tomcat-libs/* /usr/local/tomcat/lib/
ADD ./won.war /usr/local/tomcat/webapps/
ADD ./conf /usr/local/tomcat/won/conf
ADD ./setenv.sh /usr/local/tomcat/bin/
ADD ./tomcat-users.xml /usr/local/tomcat/conf/
RUN dos2unix /usr/local/tomcat/bin/setenv.sh
