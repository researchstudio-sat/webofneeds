package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.node.camel.processor.general.OutboundMessageFactoryProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    logger.debug("OPEN received from the owner side for connection {}", wonMessage.getSenderURI());

    Connection con = connectionRepository.findOneByConnectionURI(wonMessage.getSenderURI());
    Objects.requireNonNull(con);
    Objects.requireNonNull(con.getRemoteNeedURI());

    if (!con.getRemoteNeedURI().equals(wonMessage.getReceiverNeedURI())) throw new IllegalStateException("remote need uri must be equal to receiver need uri");
    if (con.getConnectionURI() == null) throw new IllegalStateException("connection uri must not be null");
    if (con.getFacetURI() == null) throw new IllegalStateException("connection's facet uri must not be null");
    if (!con.getConnectionURI().equals(wonMessage.getSenderURI())) throw new IllegalStateException("connection uri must be equal to sender uri");
    if (wonMessage.getReceiverURI() != null) {
      if (!wonMessage.getReceiverURI().equals(con.getRemoteConnectionURI()))
        throw new IllegalStateException("remote connection uri must be equal to receiver uri");
      if (con.getRemoteConnectionURI() == null) {
        //we didn't have it before, now we do:
        con.setRemoteConnectionURI(wonMessage.getReceiverURI());
      }
    } else {
      // do nothing. it's not clean, but easier to implement on the client side
      // TODO: refactor connection state and open/connect
    }
    
    // facets: the remote facet in the connection may be null before the open. 
    // check if the owner sent a remote facet. there must not be a clash
    Optional<URI> userDefinedRemoteFacetURI = Optional.ofNullable(WonRdfUtils.FacetUtils.getRemoteFacet(wonMessage));
    Optional<URI> userDefinedFacetURI = Optional.ofNullable(WonRdfUtils.FacetUtils.getFacet(wonMessage));
    failIfIsNotFacetOfNeed(userDefinedFacetURI, Optional.of(wonMessage.getSenderNeedURI()));
    failIfIsNotFacetOfNeed(userDefinedRemoteFacetURI, Optional.of(wonMessage.getReceiverNeedURI()));
    Optional<URI> connectionsRemoteFacetURI = Optional.ofNullable(con.getRemoteFacetURI());

    // check remote facet info
    if (userDefinedRemoteFacetURI.isPresent()) {
        if (connectionsRemoteFacetURI.isPresent()) {
            if (!userDefinedRemoteFacetURI.get().equals(connectionsRemoteFacetURI.get())) {
                throw new IllegalArgumentException("Cannot process OPEN FROM_OWNER: remote facet uri clashes with value already set in connection");
            }
        } else {
            // use the one from the message
            con.setRemoteFacetURI(userDefinedRemoteFacetURI.get());
        }
    } else {
        // check if neither the message nor the connection have a remote facet set 
        if (!connectionsRemoteFacetURI.isPresent()) {
            // none defined at all: look up default remote facet
            con.setRemoteFacetURI(lookupDefaultFacet(con.getRemoteNeedURI()));        
        }
    }
    failForIncompatibleFacets(con.getFacetURI(), con.getTypeURI(), con.getRemoteFacetURI());
    con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
    connectionRepository.save(con);

    URI remoteMessageUri = wonNodeInformationService
            .generateEventURI(wonMessage.getReceiverNodeURI());

    //add the facets to the message if necessary
    if (!userDefinedFacetURI.isPresent()) {
        //the user did not specify a facet uri. we have to add it
        wonMessage.addMessageProperty(WONMSG.HAS_SENDER_FACET, con.getFacetURI());
    }
    
    if (!userDefinedRemoteFacetURI.isPresent()) {
        //the user did not specify a remote uri. we have to add it
        wonMessage.addMessageProperty(WONMSG.HAS_RECEIVER_FACET, con.getRemoteFacetURI());
    }
    
    //add the information about the corresponding message to the local one
    wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, remoteMessageUri);
    //the persister will pick it up later

    //put the factory into the outbound message factory header. It will be used to generate the outbound message
    //after the wonMessage has been processed and saved, to make sure that the outbound message contains
    //all the data that we also store locally
    OutboundMessageFactory outboundMessageFactory = new OutboundMessageFactory(remoteMessageUri, con);
    exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER, outboundMessageFactory);

  }

  private URI lookupDefaultFacet(URI needURI) {
      //look up the default facet and use that one
      return WonLinkedDataUtils.getDefaultFacet(needURI, true, linkedDataSource)
              .orElseThrow(() -> new IllegalStateException("No default facet found on " + needURI));
  }
  
  private class OutboundMessageFactory extends OutboundMessageFactoryProcessor
  {
    private Connection connection;

    public OutboundMessageFactory(URI messageURI, Connection connection) {
      super(messageURI);
      this.connection = connection;
    }

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
      //create the message to send to the remote node
      return WonMessageBuilder
              .setPropertiesForPassingMessageToRemoteNode(
                      message ,
                      getMessageURI())
              .build();
    }
  }

}
