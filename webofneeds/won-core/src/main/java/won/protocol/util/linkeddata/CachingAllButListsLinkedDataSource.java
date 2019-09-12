package won.protocol.util.linkeddata;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ypanchenko Date: 06.11.2015
 */
public class CachingAllButListsLinkedDataSource extends CachingLinkedDataSource {
    // TODO instead of predefined resources should look into RDF and decide whether
    // it is a list or not
    // TODO actually the connection itself should also not be cached - i.e. status
    // can be updated
    private Pattern pattern_connections_list_uri = Pattern.compile("(.+)/connections(/)?");
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Dataset getDataForResource(URI resource) {
        if (isCachingAllowed(resource)) {
            return super.getDataForResource(resource);
        } else {
            Dataset dataset = linkedDataRestClient.readResourceData(resource);
            // TODO debug log level
            logger.info("connections list uri request performed");
            return dataset;
        }
    }

    @Override
    public Dataset getDataForResource(URI resource, URI requesterWebID) {
        if (isCachingAllowed(resource)) {
            return super.getDataForResource(resource, requesterWebID);
        } else {
            Dataset dataset = linkedDataRestClient.readResourceData(resource, requesterWebID);
            logger.info("connections list uri request performed");
            return dataset;
        }
    }

    private boolean isCachingAllowed(final Pattern noCachePattern, final URI resource) {
        Matcher matcher = noCachePattern.matcher(resource.toString());
        return !matcher.matches();
    }

    private boolean isCachingAllowed(final URI resource) {
        return isCachingAllowed(pattern_connections_list_uri, resource);
    }
}
