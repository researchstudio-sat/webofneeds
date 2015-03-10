package won.node.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;

import java.util.Map;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class WonMessageCamelProcessor implements Processor
{
  Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void process(final Exchange exchange) throws Exception {
    logger.debug("processing won message");
    Map headers = exchange.getIn().getHeaders();
    if(headers.get("wonMessage")!=null){
      WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG,headers.get("wonMessage").toString());
      exchange.getIn().setHeader("messageType",wonMessage.getMessageType().getResource());
      exchange.getIn().setHeader("wonMessage",wonMessage);
      exchange.getIn().setBody(wonMessage);
    }
  }
}
