# This image is build and pushed to the dockerhub manually (webofneeds/solr)
# The solr server should be accessible at port 8983

# use java as a base image
FROM solr:6.1.0

# add the won core to the solr instance, changed one important field in the solrconfig.xml:
# <str name="defaultFieldType">text_general</str>
# changed to text_general so that all default fields are tokenized by default.
# This is important for our querying
ADD ./core/won /opt/solr/server/solr/won

USER root
RUN chmod -R +wx /opt/solr/server/solr/won
