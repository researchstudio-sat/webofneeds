package won.protocol.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.service.RandomNumberService;
import won.protocol.service.WonNodeInformationService;

import java.net.URI;

/**
 * User: fsalcher
 * Date: 17.09.2014
 */
public class WonNodeInformationServiceImpl implements WonNodeInformationService
{

  @Autowired
  private RandomNumberService randomNumberService;

  @Override
  public WonNodeInformation getWonNodeInformation(URI wonNodeURI) {
    return getDefaultWonNodeInformation();
  }

  @Override
  public URI generateNeedMessageEventURI(URI needURI, URI wonNodeURI) {
    WonNodeInformation wonNodeInformation = getWonNodeInformation(wonNodeURI);
    return URI.create(wonNodeInformation.getNeedMessageEventURIPattern()
                                        .replace(wonNodeInformation.getNeedURIPlaceholder(), needURI.toString())
                                        .replace(wonNodeInformation.getIdPlaceholder(),
                                                 generateRandomMessageEventID()));
  }

  @Override
  public URI generateConnectionMessageEventURI(URI connectionURI, URI wonNodeURI) {
    WonNodeInformation wonNodeInformation = getWonNodeInformation(wonNodeURI);
    return URI.create(wonNodeInformation.getConnectionMessageEventURIPattern()
                                        .replace(wonNodeInformation.getConnectionURIPlaceholder(), connectionURI.toString())
                                        .replace(wonNodeInformation.getIdPlaceholder(),
                                                 generateRandomMessageEventID()));
  }

  @Override
  public URI generateConnectionURI(URI needURI, URI wonNodeURI) {
    WonNodeInformation wonNodeInformation = getWonNodeInformation(wonNodeURI);
    return URI.create(wonNodeInformation.getConnectionURIPattern()
                                        .replace(wonNodeInformation.getNeedURIPlaceholder(), needURI.toString())
                                        .replace(wonNodeInformation.getIdPlaceholder(),
                                                 generateRandomConnectionID()));
  }

  @Override
  public URI generateNeedURI(URI wonNodeURI) {
    WonNodeInformation wonNodeInformation = getWonNodeInformation(wonNodeURI);
    return URI.create(wonNodeInformation.getNeedURIPattern()
                                        .replace(wonNodeInformation.getIdPlaceholder(),
                                                 generateRandomNeedID()));
  }

  @Override
  public URI getDefaultWonNode() {
    return URI.create("http://localhost:8080/won/");
  }

  private String generateRandomMessageEventID() {
    // ToDo (FS): take length from configuration and choose good length value (maybe change value to bytes)
    return randomNumberService.generateRandomString(9);
  }

  private String generateRandomConnectionID() {
    // ToDo (FS): take length from configuration and choose good length value (maybe change value to bytes)
    return randomNumberService.generateRandomString(9);
  }

  private String generateRandomNeedID() {
    // ToDo (FS): take length from configuration and choose good length value (maybe change value to bytes)
    return randomNumberService.generateRandomString(9);
  }


  private WonNodeInformation getDefaultWonNodeInformation() {
    return new WonNodeInformation(
      "<needURI>/event/<ID>",
      "<connectionURI>/event/<ID>",
      "<needURI>/connection/<ID>",
      "http://localhost:8080/won/resource/need/<ID>",
      "<needURI>",
      "<connectionURI>",
      "<ID>");
  }

}
