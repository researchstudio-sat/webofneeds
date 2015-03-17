package won.owner.camel.processor;

import org.apache.camel.Exchange;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.OwnerProtocolOwnerServiceCallback;
import won.owner.service.impl.NopOwnerProtocolOwnerServiceCallback;
import won.protocol.jms.MessagingService;
import won.protocol.message.*;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.util.RdfUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * First processor for incoming messages. It performs integrity checks, populates
 * the exchange headers from the WoNMessage object and persists the WonMessage.
 */
public class OwnerWonMessageCamelProcessor implements WonMessageCamelProcessor
{
  Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  RDFStorageService rdfStorage;

  @Autowired
  MessagingService messagingService;

  @Autowired(required = false)
  private OwnerProtocolOwnerServiceCallback ownerServiceCallback = new NopOwnerProtocolOwnerServiceCallback();

  @Override
  public void process(final Exchange exchange) throws Exception {
    logger.debug("processing won message");

    String wonMessageString = (String) exchange.getIn().getBody();
    WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG,wonMessageString);

    if (!isIntegrityCheckOk(wonMessage, exchange)){
      return;
    }
    ownerServiceCallback.onWonMessage(wonMessage);
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
}
