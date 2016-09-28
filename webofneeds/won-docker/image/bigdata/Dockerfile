# This image is build and pushed to the dockerhub manually (webofneeds/bigdata)
# The bigdata-bundled.jar file has to be copied manually to this folder

# use java as a base image
FROM openjdk:8

# download the (manually downloaded) jar and the properties file
ADD ./bigdata-bundled.jar /usr/src/bigdata/bigdata-bundled.jar
ADD ./RWStore.properties /usr/src/bigdata/RWStore.properties

# do not make explicit volumes here, this will just fill up disk space in our integration test environment very fast
# define the data folder of the store in RWStore.properties
# VOLUME /usr/src/bigdata/data

# start bigdata on port 9999
WORKDIR /usr/src/bigdata/
CMD java -server -Xmx2g -Dbigdata.propertyFile=/usr/src/bigdata/RWStore.properties -jar bigdata-bundled.jar

