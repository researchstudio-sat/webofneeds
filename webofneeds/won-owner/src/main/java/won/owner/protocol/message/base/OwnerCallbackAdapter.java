/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.owner.protocol.message.base;

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import won.owner.protocol.message.OwnerCallback;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.util.RdfUtils;

import java.lang.invoke.MethodHandles;

/**
 * Maps incoming messages from the WonMessageProcessor interface to the
 * WonEventCallback interface. Outgoing messages sent by calling the adaptee's
 * send(msg) method are delegated to the
 */
public abstract class OwnerCallbackAdapter implements WonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private OwnerCallback adaptee;

    protected OwnerCallbackAdapter() {
    }

    public OwnerCallbackAdapter(final OwnerCallback adaptee) {
        this.adaptee = adaptee;
    }

    /**
     * Creates a connection object representing the connection on which the message
     * was received.
     *
     * @param wonMessage
     * @return
     */
    protected abstract Connection makeConnection(final WonMessage wonMessage);

    @Override
    public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
        assert adaptee != null : "adaptee is not set";
        logger.debug("processing message {} and calling appropriate method on adaptee", message.getMessageURI());
        WonMessageType messageType = message.getMessageType();
        switch (messageType) {
            case ATOM_HINT_MESSAGE:
                adaptee.onAtomHintFromMatcher(message);
                break;
            case SOCKET_HINT_MESSAGE:
                adaptee.onSocketHintFromMatcher(message);
                break;
            case CONNECT:
                adaptee.onConnectFromOtherAtom(makeConnection(message), message);
                break;
            case OPEN:
                adaptee.onOpenFromOtherAtom(makeConnection(message), message);
                break;
            case CONNECTION_MESSAGE:
                adaptee.onMessageFromOtherAtom(makeConnection(message), message);
                break;
            case CLOSE:
                adaptee.onCloseFromOtherAtom(makeConnection(message), message);
                break;
            case SUCCESS_RESPONSE:
                // logger.info("Not handling successResponse for message {}", message);
                adaptee.onSuccessResponse(message.getIsResponseToMessageURI(), message);
                break;
            case FAILURE_RESPONSE:
                adaptee.onFailureResponse(message.getIsResponseToMessageURI(), message);
                break;
            case CREATE_ATOM:
                logger.info("Handling CREATE_ATOM for message {}", message);
                break;
            default:
                logger.info("could not find callback method for wonMessage of type {}", messageType);
                if (logger.isDebugEnabled()) {
                    logger.debug("message: {}", RdfUtils.writeDatasetToString(message.getCompleteDataset(), Lang.TRIG));
                }
        }
        // return the message for further processing
        return message;
    }

    @Autowired(required = false)
    @Qualifier("default")
    public void setAdaptee(OwnerCallback adaptee) {
        this.adaptee = adaptee;
    }
}
