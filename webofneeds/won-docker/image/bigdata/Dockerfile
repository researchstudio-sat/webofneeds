# This image is build and pushed to the dockerhub manually (webofneeds/bigdata)
# The bigdata-bundled.jar file has to be copied manually to this folder

# use java as a base image
# fix java version here until the following issue is resolved: https://github.com/researchstudio-sat/webofneeds/issues/1229
FROM openjdk:8u121-jdk

# download the (manually downloaded) jar and the properties file
ADD https://sourceforge.net/projects/bigdata/files/bigdata/2.1.4/blazegraph.jar/download# /usr/src/bigdata/bigdata-bundled.jar
ADD ./RWStore.properties /usr/src/bigdata/RWStore.properties

# do not make explicit volumes here, this will just fill up disk space in our integration test environment very fast
# define the data folder of the store in RWStore.properties
# VOLUME /usr/src/bigdata/data

# start bigdata on port 9999
WORKDIR /usr/src/bigdata/
CMD java -server -Xmx2g -Djetty.start.timeout=60 -Dbigdata.propertyFile=/usr/src/bigdata/RWStore.properties -jar bigdata-bundled.jar

