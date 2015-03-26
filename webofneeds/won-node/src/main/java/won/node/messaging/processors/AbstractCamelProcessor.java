package won.node.messaging.processors;

import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.node.service.impl.NeedCommunicationServiceImpl;

/**
 * User: syim
 * Date: 02.03.2015
 */
public abstract class AbstractCamelProcessor extends ProcessorBase implements Processor
{
  protected final Logger logger = LoggerFactory.getLogger(NeedCommunicationServiceImpl.class);
}
