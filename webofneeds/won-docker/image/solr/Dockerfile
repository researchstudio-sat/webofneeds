# The solr server should be accessible at port 8983
# use java as a base image
FROM solr:6.1.0

# add the won core to the solr instance
ADD ./core/won /opt/solr/server/solr/won

# also add a duplicate of this core for testing purpose
ADD ./core/wontest  /opt/solr/server/solr/wontest
ADD ./core/won/conf /opt/solr/server/solr/wontest/conf
ADD ./core/won/data /opt/solr/server/solr/wontest/data

USER root
RUN chmod -R +wx /opt/solr/server/solr/won
RUN chmod -R +wx /opt/solr/server/solr/wontest