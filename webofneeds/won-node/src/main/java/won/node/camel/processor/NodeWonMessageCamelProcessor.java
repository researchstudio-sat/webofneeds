package won.node.camel.processor;

import org.apache.camel.Exchange;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.jms.MessagingService;
import won.protocol.message.*;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * First processor for incoming messages. It performs integrity checks, populates
 * the exchange headers from the WoNMessage object and persists the WonMessage.
 */
public class NodeWonMessageCamelProcessor implements WonMessageCamelProcessor
{
  Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  RDFStorageService rdfStorage;

  @Autowired
  MessagingService messagingService;

  @Override
  public void process(final Exchange exchange) throws Exception {
    logger.debug("processing won message");
    Map headers = exchange.getIn().getHeaders();
    WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG,exchange.getIn().getBody().toString());
    if (!isIntegrityCheckOk(wonMessage, exchange)){
      return;
    }
    WonMessage wrappedWonMessage = wrapMessage(wonMessage, WonMessageDirection.getWonMessageDirection(URI.create((String) headers.get("direction"))));
    saveMessage(wrappedWonMessage);
    populateExchangeHeadersFromMessage(exchange, wrappedWonMessage);
  }




  private void saveMessage(final WonMessage wrappedWonMessage) {
    logger.debug("STORING message with id {}", wrappedWonMessage.getMessageURI());
    rdfStorage.storeDataset(wrappedWonMessage.getMessageURI(),
                            WonMessageEncoder.encodeAsDataset(wrappedWonMessage));
  }

  private WonMessage wrapMessage(final WonMessage wonMessage, WonMessageDirection direction) {
    return WonMessageBuilder.wrapMessageReceivedByNode(wonMessage, direction);
  }

  public boolean isIntegrityCheckOk(final WonMessage wonMessage, final Exchange exchange) {
    //check message integrity, log error, send error msg back and return false if something is wrong
    if (false) {
      //will need something like this:
      //get owner app id from incoming message
      String appId = (String) exchange.getIn().getHeader("ownerApplicationID");
      //TODO: handle error - app id not found!
      //set in header of outgoing message
      Map headerMap = new HashMap<String, Object>();
      headerMap.put("ownerApplications", Arrays.asList(new String[]{appId}));
      WonMessage message = null; //create new message;
      messagingService.sendInOnlyMessage(null, headerMap, RdfUtils.writeDatasetToString(message.getCompleteDataset(),Lang.JSONLD),
                                         "outgoingMessages");
      return false;
    }
    return true;
  }

  private void populateExchangeHeadersFromMessage(final Exchange exchange, final WonMessage wonMessage) {
    exchange.getIn().setHeader("messageType", URI.create(wonMessage.getMessageType().getResource().getURI()));
    exchange.getIn().setHeader("facetType", WonRdfUtils.FacetUtils.getFacet(wonMessage));
    exchange.getIn().setBody(wonMessage);
  }


  public void setRdfStorage(final RDFStorageService rdfStorage) {
    this.rdfStorage = rdfStorage;
  }

  public void setMessagingService(final MessagingService messagingService) {
    this.messagingService = messagingService;
  }
}
