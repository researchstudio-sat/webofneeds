package won.owner.service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import won.cryptography.service.RandomNumberServiceImpl;
import won.owner.service.OwnerApplicationServiceCallback;
import won.owner.service.OwnerProtocolOwnerServiceCallback;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDecoder;
import won.protocol.message.WonMessageType;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.Match;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * User: fsalcher
 * Date: 18.08.2014
 */
public class OwnerApplicationService implements OwnerProtocolOwnerServiceCallback
{

  private static final Logger logger = LoggerFactory.getLogger(OwnerApplicationService.class);

  @Autowired
  @Qualifier("default")
  private OwnerProtocolNeedServiceClientSide ownerProtocolService;

  //when the callback is a bean in a child context, it sets itself as a dependency here
  @Autowired(required = false)
  private OwnerApplicationServiceCallback ownerApplicationServiceCallbackToClient =
    new NopOwnerApplicationServiceCallback();

  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private Executor executor;

  @Autowired
  private RandomNumberServiceImpl randomNumberService;

  final private Map<URI, WonMessage> wonMessageMap = new HashMap<>();

  // ToDo (FS): add security layer

  public void handleMessageEventFromClient(Dataset wonMessage) {
    handleMessageEventFromClient(WonMessageDecoder.decodeFromDataset(wonMessage));
  }

  public void handleMessageEventFromClient(WonMessage wonMessage) {

    // ToDo (FS): don't convert messages to the old protocol interfaces instead use the new message format

    WonMessageType wonMessageType = wonMessage.getMessageEvent().getMessageType();

    switch (wonMessageType) {
      case CREATE_NEED:

        Dataset messageContent = wonMessage.getMessageContent();

        URI senderNeedURI = wonMessage.getMessageEvent().getSenderNeedURI();
        if (senderNeedURI == null){
          throw new IllegalArgumentException("no sender need URI found!");
        }
        // get the core graph of the message for the need model
        String coreModelURIString = senderNeedURI.toString() + "/core#data";
        Model content = wonMessage.getMessageContent(coreModelURIString);

        // ToDo (FS): this should be encapsulated in an own subclass of WonMessage
        // get the active status
        boolean active = false;
        switch (WonRdfUtils.NeedUtils.queryActiveStatus(
          messageContent, wonMessage.getMessageEvent().getSenderNeedURI())) {
          case ACTIVE:
            active = true;
            break;
          case INACTIVE:
            active = false;
            break;
        }


        // get the wonNodeURI
        URI wonNodeURI = null;
        wonNodeURI = WonRdfUtils.NeedUtils.queryWonNode(messageContent);

        final ListenableFuture<URI> newNeedURI;
        try {
          wonMessageMap.put(wonMessage.getMessageEvent().getSenderNeedURI(), wonMessage);
          newNeedURI = ownerProtocolService.createNeed(content, active, wonNodeURI,wonMessage);

          newNeedURI.addListener(new Runnable()
          {
            @Override
            public void run() {
              // ToDo (FS): WON Node should return the response message
              try {
                if (newNeedURI.isDone()) {
                  sendBackResponseMessageToClient(
                    wonMessageMap.get(newNeedURI.get()), WONMSG.TYPE_RESPONSE_STATE_SUCCESS);
                } else if (newNeedURI.isCancelled()) {
                  sendBackResponseMessageToClient(
                    wonMessageMap.get(newNeedURI.get()), WONMSG.TYPE_RESPONSE_STATE_FAILURE);
                }
              } catch (InterruptedException e) {
                logger.warn("caught InterruptedException:", e);
              } catch (ExecutionException e) {
                logger.warn("caught ExecutionException:", e);
              }
            }
          }, executor);
        } catch (Exception e) {
          logger.warn("caught Exception:", e);
        }
        
        break;

      case CONNECT:
        try {
          URI needURI;
          URI otherNeedURI;

          needURI = wonMessage.getMessageEvent().getSenderNeedURI();
          otherNeedURI = wonMessage.getMessageEvent().getReceiverNeedURI();

          content = wonMessage.getMessageEvent().getModel();
          com.hp.hpl.jena.rdf.model.Model facetModel =
            WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(
              FacetType.OwnerFacet.getURI(),
              FacetType.OwnerFacet.getURI());
          content.add(facetModel);

          final ListenableFuture<URI> newConnectionURI;

          wonMessageMap.put(wonMessage.getMessageEvent().getSenderNeedURI(), wonMessage);
          newConnectionURI = ownerProtocolService.connect(needURI, otherNeedURI, content, null);

          newConnectionURI.addListener(new Runnable(){
            @Override
            public void run(){
              try {
                if (newConnectionURI.isDone()) {
                  sendBackResponseMessageToClient(
                    wonMessageMap.get(newConnectionURI.get()), WONMSG.TYPE_RESPONSE_STATE_SUCCESS);
                } else if (newConnectionURI.isCancelled()) {
                  sendBackResponseMessageToClient(
                    wonMessageMap.get(newConnectionURI.get()), WONMSG.TYPE_RESPONSE_STATE_FAILURE);
                }
              } catch (InterruptedException e) {
                logger.warn("caught InterruptedException:", e);
              } catch (ExecutionException e) {
                logger.warn("caught ExecutionException:", e);
              }
            }
          },executor);
          // ToDo (FS): change connect code such that the connectionID of the messageEvent will be used
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      case NEED_STATE:
        try {
          URI needURI;
          needURI = wonMessage.getMessageEvent().getSenderNeedURI();

          switch (wonMessage.getMessageEvent().getNewNeedState()) {
            case ACTIVE:
              ownerProtocolService.activate(needURI, wonMessage);
              break;
            case INACTIVE:
              ownerProtocolService.deactivate(needURI, wonMessage);
          }
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      case OPEN:
        try {

          senderNeedURI = wonMessage.getMessageEvent().getSenderNeedURI();
          URI receiverNeedURI = wonMessage.getMessageEvent().getReceiverNeedURI();

          List<Connection> connections =
            connectionRepository.findByNeedURIAndRemoteNeedURI(senderNeedURI, receiverNeedURI);

          URI connectionURI = connections.get(0).getConnectionURI();

          content = wonMessage.getMessageEvent().getModel();

          ownerProtocolService.open(connectionURI, content, wonMessage);
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      case CLOSE:
        try {

          senderNeedURI = wonMessage.getMessageEvent().getSenderNeedURI();
          URI receiverNeedURI = wonMessage.getMessageEvent().getReceiverNeedURI();

          List<Connection> connections =
            connectionRepository.findByNeedURIAndRemoteNeedURI(senderNeedURI, receiverNeedURI);

          URI connectionURI = connections.get(0).getConnectionURI();

          content = wonMessage.getMessageEvent().getModel();

          ownerProtocolService.close(connectionURI, content, wonMessage);
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      case CONNECTION_MESSAGE:
        try {

          senderNeedURI = wonMessage.getMessageEvent().getSenderNeedURI();
          URI receiverNeedURI = wonMessage.getMessageEvent().getReceiverNeedURI();

          List<Connection> connections =
            connectionRepository.findByNeedURIAndRemoteNeedURI(senderNeedURI, receiverNeedURI);

          URI connectionURI = connections.get(0).getConnectionURI();

          content = wonMessage.getMessageEvent().getModel();

          ownerProtocolService.sendConnectionMessage(connectionURI, content, wonMessage);
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      default:
        break;
    }
  }

  public void handleMessageEventFromWonNode(Dataset wonMessage) {
    handleMessageEventFromWonNode(WonMessageDecoder.decodeFromDataset(wonMessage));
  }

  public void handleMessageEventFromWonNode(WonMessage wonMessage) {

    // ToDo (FS): handle messages

    ownerApplicationServiceCallbackToClient.onMessage(wonMessage);

  }

  // ToDo (FS): most (all?) of the response messages should be send back from the WON node (this is only temporary)
  // this is only a CREATE RESPONSE
  private void sendBackResponseMessageToClient(WonMessage wonMessage, Resource responseType) {

    try {
      URI responseMessageURI = URI.create(wonMessage.getMessageEvent().getSenderNeedURI().toString() +
                                            "/event/" +
                                            randomNumberService
                                              .generateRandomString(9));

      WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
      WonMessage responseWonMessage = wonMessageBuilder
        .setWonMessageType(WonMessageType.CREATE_RESPONSE)
        .setMessageURI(responseMessageURI)
        .setSenderNodeURI(wonMessage.getMessageEvent().getReceiverNodeURI())
        .setReceiverNeedURI(wonMessage.getMessageEvent().getSenderNeedURI())
        .setResponseMessageState(responseType)
        .addRefersToURI(wonMessage.getMessageEvent().getMessageURI())
        .build();

      ownerApplicationServiceCallbackToClient.onMessage(responseWonMessage);
    } catch (WonMessageBuilderException e) {
      logger.warn("caught WonMessageBuilderException:", e);
    }
  }


  // ToDo (FS): methods only used until the messaging system is completely refactored then only one callback method will be used
  @Override
  public void onHint(final Match match, final Model content, final WonMessage wonMessage) {

    if (wonMessage != null) {
      ownerApplicationServiceCallbackToClient.onMessage(wonMessage);
      return;
    }

    try {

      // since we have no message URI at this point we just generate one
      Random rand = new Random();
      URI messageURI = null;
      URI contentURI = null;
      //TODO: these URIs are generated by the matcher in the final implementation (and will look different).
      messageURI = new URI(match.getOriginator().toString() + "/hintMessage/" + rand.nextInt());
      contentURI = new URI(match.getOriginator().toString() + "/hint/" + rand.nextInt());

      WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
      WonMessage newWonMessage = wonMessageBuilder
        .setWonMessageType(WonMessageType.HINT_MESSAGE)
        .setMessageURI(messageURI)
        .setSenderNodeURI(match.getOriginator())
        .setReceiverURI(match.getToNeed()) // ToDo (FS): is this the need facet?
        .setReceiverNeedURI(match.getToNeed())
        .setReceiverNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): add local WON node
        .addContent(contentURI, content, null)
        .build();

      ownerApplicationServiceCallbackToClient.onMessage(newWonMessage);

    } catch (URISyntaxException e) {
      logger.warn("caught URISyntaxException:", e);
    } catch (WonMessageBuilderException e) {
      logger.warn("caught WonMessageBuilderException:", e);
    }
  }

  @Override
  public void onConnect(final Connection con, final Model content, final WonMessage wonMessage) {

    if (wonMessage != null) {
      ownerApplicationServiceCallbackToClient.onMessage(wonMessage);
      return;
    }

    try {

      // since we have no message URI at this point we just generate one
      Random rand = new Random();
      URI messageURI = null;
      URI contentURI = null;

      messageURI = new URI(con.getRemoteConnectionURI().toString() + "/event/" + rand.nextInt());
      contentURI = new URI(con.getRemoteConnectionURI().toString() + "/eventContent/" + rand.nextInt());

      WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
      WonMessage newWonMessage = wonMessageBuilder
        .setWonMessageType(WonMessageType.CONNECT)
        .setMessageURI(messageURI)
        .setReceiverURI(con.getNeedURI()) // ToDo (FS): this should be the facet
        .setReceiverNeedURI(con.getNeedURI())
        .setReceiverNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): replace with local WON node
        .setSenderURI(con.getRemoteConnectionURI())
        .setSenderNeedURI(con.getRemoteNeedURI())
        .setSenderNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): replace with remote WON node
        .addContent(contentURI, content, null)
        .build();

      ownerApplicationServiceCallbackToClient.onMessage(newWonMessage);
    } catch (URISyntaxException e) {
      logger.warn("caught URISyntaxException:", e);
    } catch (WonMessageBuilderException e) {
      logger.warn("caught WonMessageBuilderException:", e);
    }
  }

  @Override
  public void onOpen(final Connection con, final Model content, final WonMessage wonMessage) {

    if (wonMessage != null) {
      ownerApplicationServiceCallbackToClient.onMessage(wonMessage);
      return;
    }

    try {

      // since we have no message URI at this point we just generate one
      Random rand = new Random();
      URI messageURI = null;
      URI contentURI = null;

      messageURI = new URI(con.getRemoteConnectionURI().toString() + "/event/" + rand.nextInt());
      contentURI = new URI(con.getRemoteConnectionURI().toString() + "/eventContent/" + rand.nextInt());

      WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
      WonMessage newWonMessage = wonMessageBuilder
        .setWonMessageType(WonMessageType.OPEN)
        .setMessageURI(messageURI)
        .setReceiverURI(con.getConnectionURI())
        .setReceiverNeedURI(con.getNeedURI())
        .setReceiverNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): replace by remote WON node
        .setSenderURI(con.getRemoteConnectionURI())
        .setSenderNeedURI(con.getRemoteNeedURI())
        .setSenderNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): replace by local WON node
        .addContent(contentURI, content, null)
        .build();

      ownerApplicationServiceCallbackToClient.onMessage(newWonMessage);

    } catch (URISyntaxException e) {
      logger.warn("caught URISyntaxException:", e);
    } catch (WonMessageBuilderException e) {
      logger.warn("caught WonMessageBuilderException:", e);
    }
  }

  @Override
  public void onClose(final Connection con, final Model content, final WonMessage wonMessage) {

    if (wonMessage != null) {
      ownerApplicationServiceCallbackToClient.onMessage(wonMessage);
      return;
    }

    try {

      // since we have no message URI at this point we just generate one
      Random rand = new Random();
      URI messageURI = null;
      URI contentURI = null;
      messageURI = new URI(con.getRemoteConnectionURI().toString() + "/event/" + rand.nextInt());
      contentURI = new URI(con.getRemoteConnectionURI().toString() + "/eventContent/" + rand.nextInt());

      WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
      WonMessage newWonMessage = wonMessageBuilder
        .setWonMessageType(WonMessageType.CLOSE)
        .setMessageURI(messageURI)
        .setReceiverURI(con.getConnectionURI())
        .setReceiverNeedURI(con.getNeedURI())
        .setReceiverNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): replace by remote WON node
        .setSenderURI(con.getRemoteConnectionURI())
        .setSenderNeedURI(con.getRemoteNeedURI())
        .setSenderNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): replace by local WON node
        .addContent(contentURI, content, null)
        .build();

      ownerApplicationServiceCallbackToClient.onMessage(newWonMessage);

    } catch (URISyntaxException e) {
      logger.warn("caught URISyntaxException:", e);
    } catch (WonMessageBuilderException e) {
      logger.warn("caught WonMessageBuilderException:", e);
    }
  }

  @Override
  public void onTextMessage(final Connection con, final ChatMessage message,
                            final Model content, final WonMessage wonMessage) {

    if (wonMessage != null) {
      ownerApplicationServiceCallbackToClient.onMessage(wonMessage);
      return;
    }

    try {

      // since we have no message URI at this point we just generate one
      Random rand = new Random();
      URI messageURI = null;
      URI contentURI = null;

      messageURI = new URI(con.getRemoteConnectionURI().toString() + "/event/" + rand.nextInt());
      contentURI = new URI(con.getRemoteConnectionURI().toString() + "/eventContent/" + rand.nextInt());


      WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
      WonMessage newWonMessage = wonMessageBuilder
        .setWonMessageType(WonMessageType.CONNECTION_MESSAGE)
        .setMessageURI(messageURI)
        .setReceiverURI(con.getConnectionURI())
        .setReceiverNeedURI(con.getNeedURI())
        .setReceiverNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): replace by remote WON node
        .setSenderURI(con.getRemoteConnectionURI())
        .setSenderNeedURI(con.getRemoteNeedURI())
        .setSenderNodeURI(new URI("www.coming-soon.org")) // ToDo (FS): replace by local WON node
        .addContent(contentURI, content, null)
        .build();

      // ToDo (FS): if ChatMessage content is not in the content add ChatMessage to newWonMessage

      ownerApplicationServiceCallbackToClient.onMessage(newWonMessage);

    } catch (URISyntaxException e) {
      logger.warn("caught URISyntaxException:", e);
    } catch (WonMessageBuilderException e) {
      logger.warn("caught WonMessageBuilderException:", e);
    }
  }

  public void setOwnerApplicationServiceCallbackToClient(final OwnerApplicationServiceCallback ownerApplicationServiceCallbackToClient) {
    this.ownerApplicationServiceCallbackToClient = ownerApplicationServiceCallbackToClient;
  }



}
