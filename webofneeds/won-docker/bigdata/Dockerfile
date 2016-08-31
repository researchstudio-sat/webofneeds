# This image is build and pushed to the dockerhub manually (webofneeds/bigdata)
# The bigdata-1.5.2-bundled.jar file has to be copied manually to this folder

# use java as a base image
FROM openjdk:8

# download the big-data bundled jar
ADD ./bigdata-1.5.2-bundled.jar /usr/src/bigdata/bigdata-bundled.jar

# start bigdata on port 9999
WORKDIR /usr/src/bigdata/
CMD java -server -Xmx4g -jar bigdata-bundled.jar