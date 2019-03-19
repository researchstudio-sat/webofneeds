package won.owner.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import won.protocol.message.WonMessage;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.sender.WonMessageSender;

/**
 * Service that connects client-side logic (e.g. the WonWebSocketHandler in won-owner-webapp) with facilities for
 * sending and receiving messages.
 */
public class OwnerApplicationService implements WonMessageProcessor, WonMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(OwnerApplicationService.class);

    @Autowired
    @Qualifier("default")
    private WonMessageSender wonMessageSenderDelegate;

    // when the callback is a bean in a child context, it sets itself as a dependency here
    // we don't do autowiring.
    private WonMessageProcessor messageProcessorDelegate = new NopOwnerApplicationServiceCallback();

    /**
     * Sends a message to the won node.
     * 
     * @param wonMessage
     */
    public void sendWonMessage(WonMessage wonMessage) {
        try {
            // send to node:
            wonMessageSenderDelegate.sendWonMessage(wonMessage);
        } catch (Exception e) {
            // TODO: send error message back to client!
            logger.info("could not send WonMessage", e);
        }
    }

    /**
     * Sends a message to the owner.
     * 
     * @param wonMessage
     */
    @Override
    public WonMessage process(final WonMessage wonMessage) {
        return messageProcessorDelegate.process(wonMessage);
    }

    public void setMessageProcessorDelegate(final WonMessageProcessor messageProcessorDelegate) {
        this.messageProcessorDelegate = messageProcessorDelegate;
    }

}
