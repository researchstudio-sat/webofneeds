package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Facet;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectMessageFromNodeProcessor extends AbstractCamelProcessor {

    public void process(final Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        // a need wants to connect.
        // get the required data from the message and create a connection
        URI needUri = wonMessage.getReceiverNeedURI();
        URI wonNodeUriFromWonMessage = wonMessage.getReceiverNodeURI();
        URI remoteNeedUri = wonMessage.getSenderNeedURI();
        URI remoteConnectionUri = wonMessage.getSenderURI();
        URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage);
        if (facetURI == null) {
            throw new IllegalArgumentException("cannot process FROM_EXTERNAL connect without receiverFacetURI");
        }
        failIfIsNotFacetOfNeed(Optional.of(facetURI), Optional.of(needUri));
        Facet facet = dataService.getFacet(needUri, facetURI == null ? Optional.empty() : Optional.of(facetURI));
        URI connectionURI = wonMessage.getReceiverURI(); // if the uri is known already, we can load the connection!

        // the remote facet must be specified in a message coming from another node
        URI remoteFacetURI = WonRdfUtils.FacetUtils.getRemoteFacet(wonMessage);
        failIfIsNotFacetOfNeed(Optional.of(remoteFacetURI), Optional.of(remoteNeedUri));
        // we complain about hasFacet, not hasRemoteFacet, because it's a remote message!
        if (remoteFacetURI == null)
            throw new MissingMessagePropertyException(URI.create(WONMSG.HAS_RECEIVER_FACET.toString()));
        if (remoteConnectionUri == null)
            throw new MissingMessagePropertyException(URI.create(WONMSG.SENDER_PROPERTY.getURI().toString()));

        Connection con = null;
        if (connectionURI != null) {
            // we already knew about this connection. load it
            con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI).get();
            if (con == null)
                throw new NoSuchConnectionException(connectionURI);
            if (con.getRemoteConnectionURI() != null && !remoteConnectionUri.equals(con.getRemoteConnectionURI())) {
                throw new IllegalStateException(
                        "Cannot process CONNECT message FROM_EXTERNAL. Specified connection uris conflict with existing connection data");
            }
            if (con.getRemoteFacetURI() != null && !remoteFacetURI.equals(con.getRemoteFacetURI())) {
                throw new IllegalStateException(
                        "Cannot process CONNECT message FROM_EXTERNAL. Specified facet uris conflict with existing connection data");
            }
        } else {
            // the sender did not know about our connection. try to find out if one exists that we can use

            // we know which remote facet to connect to. There may be a connection with
            // that information already, either because the hint pointed to the remote
            // facet or because the connection is already in a different state and this
            // is a duplicate connect..

            Optional<Connection> conOpt = connectionRepository
                    .findOneByNeedURIAndRemoteNeedURIAndFacetURIAndRemoteFacetURIForUpdate(needUri, remoteNeedUri,
                            facet.getFacetURI(), remoteFacetURI);
            if (conOpt.isPresent()) {
                con = conOpt.get();
            } else {
                // did not find such a connection. It could be that the connection exists, but without a remote facet
                conOpt = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndFacetURIAndNullRemoteFacetForUpdate(
                        needUri, remoteNeedUri, facet.getFacetURI());
                if (conOpt.isPresent()) {
                    // we found a connection without a remote facet uri. we use this one and we'll have to set the
                    // remote facet uri.
                    con = conOpt.get();
                } else {
                    // did not find such a connection either. We can safely create a new one. (see below)
                }
            }
        }
        failForIncompatibleFacets(facet.getFacetURI(), facet.getTypeURI(), remoteFacetURI);
        if (con == null) {
            // create Connection in Database
            URI connectionUri = wonNodeInformationService.generateConnectionURI(wonNodeUriFromWonMessage);
            con = dataService.createConnection(connectionUri, needUri, remoteNeedUri, remoteConnectionUri,
                    facet.getFacetURI(), facet.getTypeURI(), remoteFacetURI, ConnectionState.REQUEST_RECEIVED,
                    ConnectionEventType.PARTNER_OPEN);
        }
        con.setRemoteConnectionURI(remoteConnectionUri);
        con.setRemoteFacetURI(remoteFacetURI);
        con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
        connectionRepository.save(con);

        // set the receiver to the newly generated connection uri
        wonMessage.addMessageProperty(WONMSG.RECEIVER_PROPERTY, con.getConnectionURI());

    }

}
