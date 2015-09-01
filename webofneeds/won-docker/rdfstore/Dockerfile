# The bigdata-1.5.2-bundled.jar file has to be copied manually to this folder

# use java as a base image
FROM java:8

# download the big-data bundled jar
ADD ./bigdata-1.5.2-bundled.jar /usr/src/bigdata/bigdata-bundled.jar

# start bigdata
WORKDIR /usr/src/bigdata/
EXPOSE 9999
CMD java -server -Xmx4g -jar bigdata-bundled.jar