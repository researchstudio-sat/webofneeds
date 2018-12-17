package won.node.camel.processor.general;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: MS 01.12.2018
 */

public class DeleteNeedMessageProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(final Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (wonMessage.getMessageType() == WonMessageType.SUCCESS_RESPONSE
                && wonMessage.getIsResponseToMessageType() == WonMessageType.DELETE) {
            URI receiverNeedURI = wonMessage.getReceiverNeedURI();
            if (receiverNeedURI == null) {
                throw new WonMessageProcessingException("receiverNeedURI is not set");
            }
            Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);

            if (need.getState() == NeedState.DELETED) {
                // Delete Need
                logger.debug("Set need to state DELETED. needURI:{}", receiverNeedURI);
                Collection<Connection> conns = connectionRepository
                        .getConnectionsByNeedURIAndNotInStateForUpdate(need.getNeedURI(), ConnectionState.CLOSED);
                if(conns.size() > 0) {
                    //Still not closed connections
                    logger.debug("Still open connections for need. needURI{}", receiverNeedURI);
                    //TODO: Handle!
                    
                }
                
                messageEventRepository.deleteByParentURI(need.getNeedURI());
                need.resetAllNeedData();
            } else {
                // First Step: Delete message to set need in DELETED state and start delete process
                logger.debug("DELETING need. needURI:{}", receiverNeedURI);
                need.setState(NeedState.DELETED);
            }
            needRepository.save(need);
        }
    }
}
