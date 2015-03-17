package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.annotation.FixedMessageProcessor;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.*;
import won.protocol.model.*;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_DEACTIVATE_STRING)
public class DeactivateNeedMessageProcessor implements WonMessageProcessor
{
  Logger logger = LoggerFactory.getLogger(this.getClass());

  private MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;
  //used to close connections when a need is deactivated

  private RDFStorageService rdfStorage;

  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  FacetRepository facetRepository;
  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  private LinkedDataSource linkedDataSource;
  @Autowired
  private WonNodeInformationService wonNodeInformationService;


  public void process(final Exchange exchange) throws Exception {
    WonMessage wonMessage = exchange.getIn().getBody(WonMessage.class);
    WonMessage newWonMessage = WonMessageBuilder.wrapOutboundOwnerToNodeOrSystemMessageAsNodeToNodeMessage(wonMessage);
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                            WonMessageEncoder.encodeAsDataset(newWonMessage));

    URI receiverNeedURI = newWonMessage.getReceiverNeedURI();
    logger.debug("DEACTIVATING need. needURI:{}", receiverNeedURI);
    if (receiverNeedURI == null) throw new IllegalArgumentException("receiverNeedURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
    need.setState(NeedState.INACTIVE);
    need = needRepository.save(need);
    messageEventRepository.save(new MessageEventPlaceholder(need.getNeedURI(), newWonMessage));

    //close all connections
    Collection<URI> connectionURIs = connectionRepository.getConnectionURIsByNeedURIAndNotInState(need.getNeedURI
      (), ConnectionState.CLOSED);
    for (URI connURI : connectionURIs) {
      try {
        //TODO: actually, the WoN node should create a close event for the connection,
        //save it (so it can be referenced as linked data) and send that event to local
        //and remote connection
        WonMessage closeWonMessage = createCloseWonMessage(connURI,false,wonMessage );
        WonMessage closeWonMessageOwner = createCloseWonMessage(connURI,true,wonMessage );
        if (closeWonMessage != null) {
          //simulate a call from the owner so as to close the remote connection, too
          //  ownerFacingConnectionCommunicationService.close(connURI, null, closeWonMessage);
          //simulate a call from the remote connection so that owner is informed
          //TODO: in this call, the message is copied (as it is assumed to come from a remote WoN node)
          //which is redundant and should be solved differently
          //  needFacingConnectionCommunicationService.close(connURI,null,closeWonMessageOwner);
        }
      } catch (WonMessageBuilderException e) {
        logger.warn("close message could not be created", e);
      }

    }
    // ToDo (FS): define own message or forward the deactivate message?
    matcherProtocolMatcherClient.needDeactivated(need.getNeedURI(), newWonMessage);
  }

  //TODO is this the right place for thie method?
  private WonMessage createCloseWonMessage(URI connectionURI, final boolean fromDeactivate, WonMessage wonMessage)
    throws WonMessageBuilderException {

    List<Connection> connections = connectionRepository.findByConnectionURI(connectionURI);
    if (connections.size() != 1)
      throw new IllegalArgumentException("no or too many connections found for ID " + connectionURI.toString());

    Connection connection = connections.get(0);
    if (!ConnectionState.closeOnNeedDeactivate(connection.getState())) return null;

    URI localWonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(connection.getConnectionURI(),
                                                                                 linkedDataSource);
    URI remoteWonNodeUri = null;
    if (connection.getRemoteConnectionURI() != null) {
      if(fromDeactivate){
        WonMessageBuilder builder = new WonMessageBuilder();
        return builder.forward(wonMessage).setMessagePropertiesForClose(
          wonNodeInformationService.generateEventURI(),
          connection.getConnectionURI(),
          connection.getNeedURI(),
          localWonNodeUri,
          connection.getConnectionURI(),
          connection.getNeedURI(),
          localWonNodeUri)
                      .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
                      .build();

      }else{
        remoteWonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(connection.getRemoteConnectionURI(),
                                                                                  linkedDataSource);
        WonMessageBuilder builder = new WonMessageBuilder();
        return builder.forward(wonMessage).setMessagePropertiesForClose(
          wonNodeInformationService.generateEventURI(),
          connection.getConnectionURI(),
          connection.getNeedURI(),
          localWonNodeUri,
          connection.getRemoteConnectionURI(),
          connection.getRemoteNeedURI(),
          remoteWonNodeUri)
                      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
                      .build();
      }

    } else {
      WonMessageBuilder builder = new WonMessageBuilder();
      return builder.forward(wonMessage).setMessagePropertiesForLocalOnlyClose(
        wonNodeInformationService.generateEventURI(),
        connection.getConnectionURI(),
        connection.getNeedURI(),
        localWonNodeUri)
                    .build();

    }


  }
}
