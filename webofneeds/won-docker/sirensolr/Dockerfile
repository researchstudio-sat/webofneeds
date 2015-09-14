# This image is build and pushed to the dockerhub manually (webofneeds/sirensolr)
# The siren-solr-1.4-bin zip file has to be copied and extracted manually to this folder

# use java as a base image
FROM java:8

# add the siren/solr
ADD ./siren-solr-1.4-bin /usr/src/siren-solr

# add the won core to the siren/solr instance
ADD ./core/won /usr/src/siren-solr/example/solr/won

# start siren/solr server
WORKDIR /usr/src/siren-solr/example
EXPOSE 8983
CMD java -jar start.jar