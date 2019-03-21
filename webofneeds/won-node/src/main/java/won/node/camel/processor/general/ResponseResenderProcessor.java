package won.node.camel.processor.general;

import org.apache.camel.Exchange;
import org.apache.jena.query.Dataset;
import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.MessageEventPlaceholder;

import java.net.URI;

/**
 * User: ypanchenko Date: 27.04.2015
 */
public class ResponseResenderProcessor extends AbstractCamelProcessor {
  @Override
  public void process(final Exchange exchange) throws Exception {
    WonMessage originalMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.ORIGINAL_MESSAGE_HEADER);

    if (originalMessage == null) {
      logger.debug("Processing an exception. camel header {} was null, assuming original message in header {}",
          WonCamelConstants.ORIGINAL_MESSAGE_HEADER, WonCamelConstants.MESSAGE_HEADER);
      originalMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    }
    if (originalMessage == null) {
      logger.warn("Could not obtain original message from camel headers {} or {} for error {}",
          new Object[] { WonCamelConstants.ORIGINAL_MESSAGE_HEADER, WonCamelConstants.MESSAGE_HEADER,
              exchange.getProperty(Exchange.EXCEPTION_CAUGHT) });
      return;
    }

    logger.warn("an error occurred while processing WON message {}", originalMessage.getMessageURI());

    // get the event that was found to be already processed
    MessageEventPlaceholder event = messageEventRepository.findOneByMessageURI(originalMessage.getMessageURI());
    // get response to this event:
    URI responseURI = event.getResponseMessageURI();
    Dataset responseDataset = event.getDatasetHolder().getDataset();
    WonMessage responseMessage = new WonMessage(responseDataset);

    Exception e = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);

    if (WonMessageDirection.FROM_OWNER == originalMessage.getEnvelopeType()) {
      sendSystemMessageToOwner(responseMessage);
    } else if (WonMessageDirection.FROM_EXTERNAL == originalMessage.getEnvelopeType()) {
      sendSystemMessage(responseMessage);
    } else {
      logger.info(String.format(
          "cannot resend response message for direction of original message, "
              + "expected FROM_OWNER or FROM_EXTERNAL, but found %s. Original cause is logged.",
          originalMessage.getEnvelopeType()), e);
    }
  }
}
