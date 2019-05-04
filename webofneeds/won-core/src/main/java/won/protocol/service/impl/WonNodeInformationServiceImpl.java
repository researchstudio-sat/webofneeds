package won.protocol.service.impl;

import java.net.URI;

import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import won.cryptography.service.RandomNumberService;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * User: fsalcher Date: 17.09.2014
 */
public class WonNodeInformationServiceImpl implements WonNodeInformationService {
    private static final int RANDOM_ID_STRING_LENGTH = 20;
    @Autowired
    private RandomNumberService randomNumberService;
    @Autowired
    private LinkedDataSource linkedDataSource;
    @Value(value = "${uri.node.default}")
    private URI defaultWonNodeUri;

    @Override
    public WonNodeInfo getWonNodeInformation(URI wonNodeURI) {
        assert wonNodeURI != null;
        Dataset nodeDataset = linkedDataSource.getDataForResource(wonNodeURI);
        WonNodeInfo info = WonRdfUtils.WonNodeUtils.getWonNodeInfo(wonNodeURI, nodeDataset);
        if (info == null)
            throw new IllegalStateException("Could not obtain WonNodeInformation for URI " + wonNodeURI);
        return info;
    }

    @Override
    public URI generateAtomURI() {
        return generateAtomURI(getDefaultWonNodeURI());
    }

    @Override
    public URI generateEventURI(URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return URI.create(wonNodeInformation.getEventURIPrefix() + "/" + generateRandomID());
    }

    @Override
    public boolean isValidEventURI(URI eventURI) {
        return isValidEventURI(eventURI, getDefaultWonNodeURI());
    }

    @Override
    public boolean isValidEventURI(URI eventURI, URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return isValidURI(eventURI, wonNodeInformation.getEventURIPrefix());
    }

    @Override
    public URI generateConnectionURI() {
        return generateConnectionURI(getDefaultWonNodeURI());
    }

    @Override
    public URI generateConnectionURI(URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return URI.create(wonNodeInformation.getConnectionURIPrefix() + "/" + generateRandomID());
    }

    @Override
    public boolean isValidConnectionURI(URI connectionURI) {
        return isValidConnectionURI(connectionURI, getDefaultWonNodeURI());
    }

    @Override
    public boolean isValidConnectionURI(URI connectionURI, URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return isValidURI(connectionURI, wonNodeInformation.getConnectionURIPrefix());
    }

    @Override
    public URI generateEventURI() {
        return generateEventURI(getDefaultWonNodeURI());
    }

    @Override
    public URI generateAtomURI(URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return URI.create(wonNodeInformation.getAtomURIPrefix() + "/" + generateRandomID());
    }

    @Override
    public boolean isValidAtomURI(URI atomURI) {
        return isValidAtomURI(atomURI, getDefaultWonNodeURI());
    }

    @Override
    public boolean isValidAtomURI(URI atomURI, URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return isValidURI(atomURI, wonNodeInformation.getAtomURIPrefix());
    }

    private boolean isValidURI(URI uri, String prefix) {
        return uri != null && uri.toString().startsWith(prefix);
    }

    @Override
    public URI getWonNodeUri(final URI resourceURI) {
        URI wonNodeURI = WonLinkedDataUtils.getWonNodeURIForAtomOrConnectionURI(resourceURI, linkedDataSource);
        if (wonNodeURI == null)
            throw new IllegalStateException("Could not obtain WoN node URI for resource " + resourceURI);
        return wonNodeURI;
    }

    @Override
    public URI getDefaultWonNodeURI() {
        return defaultWonNodeUri;
    }

    public void setDefaultWonNodeUri(final URI defaultWonNodeUri) {
        this.defaultWonNodeUri = defaultWonNodeUri;
    }

    public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    public void setRandomNumberService(RandomNumberService randomNumberService) {
        this.randomNumberService = randomNumberService;
    }

    /**
     * Returns a random string that does not start with a number. We do this so that
     * we generate URIs for which prefixing will always work with N3.js
     * https://github.com/RubenVerborgh/N3.js/issues/121
     * 
     * @return
     */
    private String generateRandomID() {
        return randomNumberService.generateRandomString(RANDOM_ID_STRING_LENGTH);
    }
}
