package won.auth.linkeddata;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.rest.AuthEnabledLinkedDataRestClient;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.util.linkeddata.CachingLinkedDataSource;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Set;

public class AuthEnabledLinkedDataSource extends CachingLinkedDataSource {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public AuthEnabledLinkedDataSource() {
    }

    protected AuthEnabledLinkedDataRestClient getLinkedDataRestClient() {
        return (AuthEnabledLinkedDataRestClient) this.linkedDataRestClient;
    }

    /**
     * Obtains the description of the reseource identified by the given resourceURI,
     * using the specified authToken to authenticate.
     *
     * @param resource
     * @param authToken
     * @return
     */
    public Dataset getDataForResource(URI resource, final String authToken) {
        if (resource == null || authToken == null) {
            throw new IllegalArgumentException("resource and authToken must not be null");
        }
        resource = wonMessageUriResolver.toLocalMessageURI(resource, this);
        logger.debug("fetching linked data for URI {} with authToken (protected)", resource);
        Dataset dataset = DatasetFactory.createGeneral();
        try {
            dataset = getLinkedDataRestClient().readResourceData(resource, authToken);
            if (logger.isDebugEnabled()) {
                logger.debug("fetched resource {} with authToken (protected):", resource);
                RDFDataMgr.write(System.out, dataset, Lang.TRIG);
            }
        } catch (LinkedDataFetchingException e) {
            throw e;
        } catch (Exception e) {
            logger.debug(String.format("Couldn't fetch resource %s", resource), e);
        }
        return dataset;
    }

    public Set<String> getAuthTokens(URI resource, URI webId) {
        if (resource == null || webId == null) {
            throw new IllegalArgumentException("resource and webId must not be null");
        }
        resource = wonMessageUriResolver.toLocalMessageURI(resource, this);
        logger.debug("fetching linked data for URI {} with webid {}", resource, webId);
        Set<String> tokens = null;
        try {
            tokens = getLinkedDataRestClient().readAccessTokens(resource, webId);
            if (logger.isDebugEnabled()) {
                logger.debug("fetched resource {} with webId {}:", resource, webId);
            }
        } catch (LinkedDataFetchingException e) {
            throw e;
        } catch (Exception e) {
            logger.debug(String.format("Couldn't fetch authToken(s) %s using webId %", resource, webId), e);
        }
        return tokens;
    }

    public Set<String> getAuthTokens(URI resource, String authToken) {
        if (resource == null || authToken == null) {
            throw new IllegalArgumentException("resource and authToken must not be null");
        }
        resource = wonMessageUriResolver.toLocalMessageURI(resource, this);
        logger.debug("fetching linked data for URI {} with webid (protected)", resource);
        Set<String> tokens = null;
        try {
            tokens = getLinkedDataRestClient().readAccessTokens(resource, authToken);
            if (logger.isDebugEnabled()) {
                logger.debug("fetched resource {} with authToken (protected)", resource);
            }
        } catch (LinkedDataFetchingException e) {
            throw e;
        } catch (Exception e) {
            logger.debug(String.format("Couldn't fetch authToken(s) %s using authToken %", resource, authToken), e);
        }
        return tokens;
    }
}
