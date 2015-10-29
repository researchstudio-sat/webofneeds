# before docker build can be executed, the war file, the tomcat libs folder
# and the conf directory have to be copied into this folder (done by maven build)
FROM tomcat:8.0-jre8
RUN apt-get update && apt-get install -y \
    vim \
    less \
	dos2unix

# install oracle jre 8 latest version
RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list
RUN "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
RUN apt-get update -y
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install oracle-java8-installer -y
RUN apt-get install oracle-java8-set-default

# compile APR library to use for tomcat ssl connector
RUN cd /usr/local/tomcat/bin/
RUN tar -xf /usr/local/tomcat/bin/tomcat-native.tar.gz -C /usr/local/tomcat/bin/
RUN ls -l /usr/local/tomcat/bin/
RUN  apt-get update && apt-get install libapr1-dev libssl-dev gcc make -y
RUN cd /usr/local/tomcat/bin/tomcat-native-1.1.33-src/jni/native/ && ./configure --with-apr=/usr --with-java-home=/usr/lib/jvm/java-8-oracle && make && make install

# add webofneeds default config env variables
ENV WON_CONFIG_DIR=/usr/local/tomcat/won/conf
ENV LOGBACK_CONFIG=logback_info.xml

# add the webofneeds files
ADD ./tomcat-libs/* /usr/local/tomcat/lib/
ADD ./owner.war /usr/local/tomcat/webapps/
ADD ./conf /usr/local/tomcat/won/conf

# configure tomcat
ADD ./setenv.sh /usr/local/tomcat/bin/
# uncomment the following line to access tomcat manager with admin user
#ADD ./tomcat-users.xml /usr/local/tomcat/conf/

# configure tomcat for ssl
ADD ./ssl/server.xml /usr/local/tomcat/conf/
ADD ./ssl/t-cert.pem /usr/local/tomcat/conf/ssl/
ADD ./ssl/t-key.pem /usr/local/tomcat/conf/ssl/

RUN dos2unix /usr/local/tomcat/bin/setenv.sh
