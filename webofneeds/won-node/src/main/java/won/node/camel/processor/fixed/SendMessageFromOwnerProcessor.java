package won.node.camel.processor.fixed;

import java.net.URI;

import javax.persistence.EntityManager;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.ConnectionMessageString)
public class SendMessageFromOwnerProcessor extends AbstractCamelProcessor {
    @Autowired
    EntityManager entityManager;

    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI senderSocket = wonMessage.getSenderSocketURIRequired();
        URI recipientSocket = wonMessage.getRecipientSocketURIRequired();
        Connection con = connectionRepository
                        .findOneBySocketURIAndTargetSocketURIForUpdate(senderSocket, recipientSocket).get();
        entityManager.refresh(con);
        if (con.getState() != ConnectionState.CONNECTED) {
            throw new IllegalMessageForConnectionStateException(con.getConnectionURI(), "CONNECTION_MESSAGE",
                            con.getState());
        }
    }
}
