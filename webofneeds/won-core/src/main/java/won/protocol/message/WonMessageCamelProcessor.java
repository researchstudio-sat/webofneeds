package won.protocol.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * User: syim
 * Date: 17.03.2015
 */
public interface WonMessageCamelProcessor extends Processor
{
  boolean isIntegrityCheckOk(final WonMessage wonMessage, final Exchange exchange);
}
