package won.protocol.service.impl;

import com.hp.hpl.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import won.cryptography.service.RandomNumberService;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;

/**
 * User: fsalcher
 * Date: 17.09.2014
 */
public class WonNodeInformationServiceImpl implements WonNodeInformationService
{

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
    if (info == null) throw new IllegalStateException("Could not obtain WonNodeInformation for URI " + wonNodeURI);
    return info;
  }

  @Override
  public URI generateNeedURI() {
    return generateNeedURI(getDefaultWonNodeURI());
  }

  @Override
  public URI generateEventURI(URI wonNodeURI) {
    WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
    return URI.create(wonNodeInformation.getEventURIPrefix() + "/"+
                                          generateRandomID());
  }

  @Override
  public URI generateConnectionURI() {
    return generateConnectionURI(getDefaultWonNodeURI());
  }

  @Override
  public URI generateConnectionURI(URI wonNodeURI) {
    WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
    return URI.create(wonNodeInformation.getConnectionURIPrefix() +"/"+ generateRandomID());
  }

  @Override
  public URI generateEventURI() {
    return generateEventURI(getDefaultWonNodeURI());
  }

  @Override
  public URI generateNeedURI(URI wonNodeURI) {
    WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
    return URI.create(wonNodeInformation.getNeedURIPrefix() + "/"+
                                          generateRandomID());
  }

  @Override
  public URI getWonNodeUri(final URI resourceURI) {
    URI wonNodeURI = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(resourceURI, linkedDataSource);
    if (wonNodeURI == null) throw new
        IllegalStateException("Could not obtain WoN node URI for resource " + resourceURI);
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

  public void setRandomNumberService(RandomNumberService randomNumberService) { this.randomNumberService = randomNumberService; }

  private String generateRandomID() {
    return randomNumberService.generateRandomString(RANDOM_ID_STRING_LENGTH);
  }
}
