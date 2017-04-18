# The solr server should be accessible at port 8983
# use java as a base image
FROM solr:6.5.0

# add the won core to the solr instance
ADD ./core/won /opt/solr/server/solr/won

# also add a duplicate of this core for testing purpose
ADD ./core/wontest  /opt/solr/server/solr/wontest
ADD ./core/won/conf /opt/solr/server/solr/wontest/conf
ADD ./core/won/data /opt/solr/server/solr/wontest/data

# use the root user (instead solr user) to run the solr for now, cause the directories that are created by the doctor containers will also be root
USER root
RUN chown -R solr /opt/solr/server/solr/won
RUN chown -R solr /opt/solr/server/solr/wontest
USER solr
