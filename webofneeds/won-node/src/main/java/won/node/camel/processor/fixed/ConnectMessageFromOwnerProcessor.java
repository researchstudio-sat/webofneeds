package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.node.camel.processor.general.OutboundMessageFactoryProcessor;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Facet;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectMessageFromOwnerProcessor extends AbstractCamelProcessor {
    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI senderNeedURI = wonMessage.getSenderNeedURI();
        URI senderNodeURI = wonMessage.getSenderNodeURI();
        URI receiverNeedURI = wonMessage.getReceiverNeedURI();
        // this is a connect from owner. We allow owners to omit facets for ease of use.
        // If local or remote facets were not specified, we define them now.
        Optional<URI> userDefinedFacetURI = Optional.ofNullable(WonRdfUtils.FacetUtils.getFacet(wonMessage));
        failIfIsNotFacetOfNeed(userDefinedFacetURI, Optional.of(senderNeedURI));
        Optional<URI> userDefinedRemoteFacetURI = Optional
                        .ofNullable(WonRdfUtils.FacetUtils.getRemoteFacet(wonMessage));
        failIfIsNotFacetOfNeed(userDefinedRemoteFacetURI, Optional.of(receiverNeedURI));
        Optional<URI> connectionURI = Optional.ofNullable(wonMessage.getSenderURI()); // if the uri is known already, we
                                                                                      // can
                                                                                      // load the connection!
        Optional<Connection> con;
        if (connectionURI.isPresent()) {
            // we know the connection: load it
            con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI.get());
            if (!con.isPresent())
                throw new NoSuchConnectionException(connectionURI.get());
            // however, if the facets don't match, we report an error:
            if (userDefinedFacetURI.isPresent() && !userDefinedFacetURI.equals(con.get().getFacetURI())) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_OWNER. Specified facet uri conflicts with existing connection data");
            }
            // remote facet uri: may be set on the connection, in which case we may have a
            // conflict
            if (con.get().getRemoteFacetURI() != null && userDefinedRemoteFacetURI != null
                            && !con.get().getRemoteFacetURI().equals(userDefinedRemoteFacetURI)) {
                throw new IllegalStateException(
                                "Cannot process CONNECT message FROM_OWNER. Specified remote facet uri conflicts with existing connection data");
            }
            // if the remote facet is not yet set on the connection, we have to set it now.
            if (con.get().getRemoteFacetURI() == null) {
                con.get().setRemoteFacetURI(
                                userDefinedRemoteFacetURI.orElse(lookupDefaultFacet(con.get().getRemoteNeedURI())));
            }
            // facets are set in the connection now.
        } else {
            // we did not know about this connection. try to find out if one exists that we
            // can use
            // the effect of connect should not be surprising. either use specified facets
            // (if they are) or use default facets.
            // don't try to be clever and look for suggested connections with other facets
            // because that leads
            // consecutive connects opening connections between different facets
            //
            // hence, we can determine our facets now, before looking at what's there.
            Facet actualFacet = dataService.getFacet(senderNeedURI, userDefinedFacetURI);
            Optional<URI> actualFacetURI = Optional.of(actualFacet.getFacetURI());
            Optional<URI> actualRemoteFacetURI = Optional
                            .of(userDefinedRemoteFacetURI.orElse(lookupDefaultFacet(receiverNeedURI)));
            con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndFacetURIAndRemoteFacetURIForUpdate(
                            senderNeedURI, receiverNeedURI, actualFacetURI.get(), actualRemoteFacetURI.get());
            if (!con.isPresent()) {
                // did not find such a connection. It could be the connection exists, but
                // without a remote facet
                con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndFacetURIAndNullRemoteFacetForUpdate(
                                senderNeedURI, receiverNeedURI, actualFacetURI.get());
                if (con.isPresent()) {
                    // we found a connection without a remote facet uri. we use this one and we'll
                    // have to set the remote facet uri.
                    con.get().setRemoteFacetURI(actualRemoteFacetURI.get());
                } else {
                    // did not find such a connection either. We can safely create a new one
                    // create Connection in Database
                    URI connectionUri = wonNodeInformationService.generateConnectionURI(senderNodeURI);
                    con = Optional.of(dataService.createConnection(connectionUri, senderNeedURI, receiverNeedURI, null,
                                    actualFacet.getFacetURI(), actualFacet.getTypeURI(), actualRemoteFacetURI.get(),
                                    ConnectionState.REQUEST_SENT, ConnectionEventType.OWNER_OPEN));
                }
            }
        }
        failForIncompatibleFacets(con.get().getFacetURI(), con.get().getTypeURI(), con.get().getRemoteFacetURI());
        // state transiation
        con.get().setState(con.get().getState().transit(ConnectionEventType.OWNER_OPEN));
        connectionRepository.save(con.get());
        // prepare the message to pass to the remote node
        URI remoteMessageUri = wonNodeInformationService.generateEventURI(wonMessage.getReceiverNodeURI());
        // set the sender uri in the envelope TODO: TwoMsgs: do not set sender here
        wonMessage.addMessageProperty(WONMSG.SENDER_PROPERTY, con.get().getConnectionURI());
        // add the information about the new local connection to the original message
        wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, remoteMessageUri);
        // the persister will pick it up later
        // add the facets to the message if necessary
        if (!userDefinedFacetURI.isPresent()) {
            // the user did not specify a facet uri. we have to add it
            wonMessage.addMessageProperty(WONMSG.HAS_SENDER_FACET, con.get().getFacetURI());
        }
        if (!userDefinedRemoteFacetURI.isPresent()) {
            // the user did not specify a remote uri. we have to add it
            wonMessage.addMessageProperty(WONMSG.HAS_RECEIVER_FACET, con.get().getRemoteFacetURI());
        }
        // put the factory into the outbound message factory header. It will be used to
        // generate the outbound message
        // after the wonMessage has been processed and saved, to make sure that the
        // outbound message contains
        // all the data that we also store locally
        OutboundMessageFactory outboundMessageFactory = new OutboundMessageFactory(remoteMessageUri, con.get());
        message.setHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER, outboundMessageFactory);
    }

    private URI lookupDefaultFacet(URI needURI) {
        // look up the default facet and use that one
        return WonLinkedDataUtils.getDefaultFacet(needURI, true, linkedDataSource)
                        .orElseThrow(() -> new IllegalStateException("No default facet found on " + needURI));
    }

    private class OutboundMessageFactory extends OutboundMessageFactoryProcessor {
        private final Connection connection;

        public OutboundMessageFactory(URI messageURI, Connection connection) {
            super(messageURI);
            this.connection = connection;
        }

        @Override
        public WonMessage process(WonMessage message) throws WonMessageProcessingException {
            return WonMessageBuilder.setPropertiesForPassingMessageToRemoteNode(message, getMessageURI())
                            .setSenderURI(connection.getConnectionURI()).build();
        }
    }
}
