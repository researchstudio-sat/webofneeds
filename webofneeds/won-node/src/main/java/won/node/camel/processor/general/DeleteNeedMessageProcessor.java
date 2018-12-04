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
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.List;

/**
 * User: syim Date: 02.03.2015
 */

public class DeleteNeedMessageProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(final Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (wonMessage.getMessageType() == WonMessageType.SUCCESS_RESPONSE
                && wonMessage.getIsResponseToMessageType() == WonMessageType.DELETE) {
            URI receiverNeedURI = wonMessage.getReceiverNeedURI();
            logger.debug("DELETING need. needURI:{}", receiverNeedURI);
            if (receiverNeedURI == null)
                throw new WonMessageProcessingException("receiverNeedURI is not set");
            Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
            //need.getEventContainer().getEvents()
            //        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
            need.setState(NeedState.DELETED);
            // need = needRepository.save(need);
            // TODO Remove all data from need

            // Remove all events
            
            messageEventRepository.deleteByParentURI(need.getNeedURI());
            

            need.resetAllNeedData();

            needRepository.save(need);
        }

    }

}
