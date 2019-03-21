package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Facet;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Optional;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromNodeProcessor extends AbstractCamelProcessor {

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    Optional<URI> connectionURIFromWonMessage = Optional.ofNullable(wonMessage.getReceiverURI());
    Optional<Connection> con = Optional.empty();
    if (!connectionURIFromWonMessage.isPresent()) {
      // the opener didn't know about the connection
      // this happens, for example, when both parties get a hint. Both create a
      // connection, but they don't know
      // about each other.
      // That's why we first try to find a connection with the same needs and facet:

      // let's extract the facet, we'll need it multiple times here.
      // As the call is coming from the node, it must be present
      // (the node fills it in if the owner leaves it out)
      Optional<URI> facetURI = Optional.of(WonRdfUtils.FacetUtils.getFacet(wonMessage));
      failIfIsNotFacetOfNeed(facetURI, Optional.of(wonMessage.getReceiverNeedURI()));
      Optional<URI> remoteFacetURI = Optional.of(WonRdfUtils.FacetUtils.getRemoteFacet(wonMessage));
      failIfIsNotFacetOfNeed(remoteFacetURI, Optional.of(wonMessage.getSenderNeedURI()));

      if (!facetURI.isPresent())
        throw new IllegalArgumentException("Cannot process OPEN FROM_EXTERNAl as no facet information is present");
      if (!remoteFacetURI.isPresent())
        throw new IllegalArgumentException(
            "Cannot process OPEN FROM_EXTERNAl as no remote facet information is present");

      con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndFacetURIAndRemoteFacetURIForUpdate(
          wonMessage.getReceiverNeedURI(), wonMessage.getSenderNeedURI(), facetURI.get(), remoteFacetURI.get());
      if (!con.isPresent()) {
        // maybe we did not know about the remotefacet yet. let's try that:
        con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndFacetURIAndNullRemoteFacetForUpdate(
            wonMessage.getReceiverNeedURI(), wonMessage.getSenderNeedURI(), facetURI.get());
      }
      if (!con.isPresent()) {
        Facet facet = dataService.getFacet(wonMessage.getReceiverNeedURI(), facetURI);
        // ok, we really do not know about the connection. create it.
        URI connectionUri = wonNodeInformationService.generateConnectionURI(wonMessage.getReceiverNodeURI());
        con = Optional.of(dataService.createConnection(connectionUri, wonMessage.getReceiverNeedURI(),
            wonMessage.getSenderNeedURI(), wonMessage.getSenderURI(), facet.getFacetURI(), facet.getTypeURI(),
            remoteFacetURI.get(), ConnectionState.REQUEST_RECEIVED, ConnectionEventType.PARTNER_OPEN));
      }
    } else {
      // the opener knew about the connection. just load it.
      con = connectionRepository.findOneByConnectionURIForUpdate(connectionURIFromWonMessage.get());
    }
    // now perform checks
    if (!con.isPresent())
      throw new IllegalStateException("connection must not be null");
    if (con.get().getRemoteNeedURI() == null)
      throw new IllegalStateException("remote need uri must not be null");
    if (!con.get().getRemoteNeedURI().equals(wonMessage.getSenderNeedURI()))
      throw new IllegalStateException(
          "the remote need uri of the connection must be equal to the sender need uri of the message");
    if (wonMessage.getSenderURI() == null)
      throw new IllegalStateException("the sender uri must not be null");
    // it is possible that we didn't store the reference to the remote conneciton
    // yet. Now we can do it.
    if (con.get().getRemoteConnectionURI() == null) {
      // Set it from the message (it's the sender of the message)
      con.get().setRemoteConnectionURI(wonMessage.getSenderURI());
    }
    if (!con.get().getRemoteConnectionURI().equals(wonMessage.getSenderURI()))
      throw new IllegalStateException("the sender uri of the message must be equal to the remote connection uri");
    failForIncompatibleFacets(con.get().getFacetURI(), con.get().getTypeURI(), con.get().getRemoteFacetURI());
    con.get().setState(con.get().getState().transit(ConnectionEventType.PARTNER_OPEN));
    connectionRepository.save(con.get());

    // set the receiver to the local connection uri
    wonMessage.addMessageProperty(WONMSG.RECEIVER_PROPERTY, con.get().getConnectionURI());
  }

}
