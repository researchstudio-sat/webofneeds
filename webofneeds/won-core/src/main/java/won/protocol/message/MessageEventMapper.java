package won.protocol.message;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import won.protocol.model.NeedState;
import won.protocol.util.ModelMapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.List;

/**
 * User: ypanchenko
 * Date: 13.08.2014
 */
public class MessageEventMapper implements ModelMapper<MessageEvent>
  {

    @Override
    public Model toModel(MessageEvent messageEvent)
    {
      //TODO how to add prefix mapping?

      // create message event resource and its type triple
      Model model = ModelFactory.createDefaultModel();
      Resource messageResource = model.createResource(messageEvent.getMessageURI().toString());
      messageResource.addProperty(RDF.type, messageEvent.getMessageType().getResource());

      // create sender triple
      if (messageEvent.getSenderURI() != null) {
        messageResource.addProperty(
          WONMSG.SENDER_PROPERTY,
          model.createResource(messageEvent.getSenderURI().toString()));
      }
      if (messageEvent.getSenderNeedURI() != null) {
        messageResource.addProperty(
          WONMSG.SENDER_NEED_PROPERTY,
          model.createResource(messageEvent.getSenderNeedURI().toString()));
      }
      if (messageEvent.getSenderNodeURI() != null) {
        messageResource.addProperty(
          WONMSG.SENDER_NODE_PROPERTY,
          model.createResource(messageEvent.getSenderNodeURI().toString()));
      }

      // create receiver triple
      if (messageEvent.getReceiverURI() != null) {
        messageResource.addProperty(
          WONMSG.RECEIVER_PROPERTY,
          model.createResource(messageEvent.getReceiverURI().toString()));
      }
      if (messageEvent.getReceiverNeedURI() != null) {
        messageResource.addProperty(
          WONMSG.RECEIVER_NEED_PROPERTY,
          model.createResource(messageEvent.getReceiverNeedURI().toString()));
      }
      if (messageEvent.getReceiverNodeURI() != null) {
        messageResource.addProperty(
          WONMSG.RECEIVER_NODE_PROPERTY,
          model.createResource(messageEvent.getReceiverNodeURI().toString()));
      }

      // create hasResponseState triple
      if (messageEvent.getResponseState() != null) {
        messageResource.addProperty(
          WONMSG.HAS_RESPONSE_STATE_PROPERTY,
          model.createResource(messageEvent.getResponseState().toString()));
      }

      // create new need state triple
      if (messageEvent.getNewNeedState() != null) {
        messageResource.addProperty(
          WONMSG.NEW_NEED_STATE_PROPERTY,
          model.createResource(messageEvent.getNewNeedState().getURI().toString()));
      }

      // create hasContent triples
      for (URI internalRef : messageEvent.getHasContent()) {
        messageResource.addProperty(
          WONMSG.HAS_CONTENT_PROPERTY,
          model.createResource(internalRef.toString()));
      }

      // create refersTo triples
      for (URI externalRef : messageEvent.getRefersTo()) {
        messageResource.addProperty(
          WONMSG.REFERS_TO_PROPERTY,
          model.createResource(externalRef.toString()));
      }

      // create signatures
      for (URI uriOfSignedGraph : messageEvent.getSignatures().keySet()) {
        // add signature model triples to event triples
        RdfUtils.addAllStatements(model, messageEvent.getSignatures().get(uriOfSignedGraph));
        RdfUtils.addPrefixMapping(model, messageEvent.getSignatures().get(uriOfSignedGraph));
      }

      return model;
    }

    @Override
    public MessageEvent fromModel(Model model)
    {
      MessageEvent msgEvent = new MessageEvent();

      msgEvent.setModel(model);

      // extract message event URI and message type
      WonMessageType type = null;
      Resource eventRes = null;
      Resource responseState = null;
      StmtIterator stmtIterator = model.listStatements(null, RDF.type, RdfUtils.EMPTY_RDF_NODE);
      while (stmtIterator.hasNext()) {
        Statement stmt = stmtIterator.nextStatement();
        if (stmt.getObject().equals(WONMSG.TYPE_CREATE)) {
          type = WonMessageType.CREATE_NEED;
        } else if (stmt.getObject().equals(WONMSG.TYPE_CREATE_RESPONSE)) {
           type = WonMessageType.CREATE_RESPONSE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_CONNECT)) {
           type = WonMessageType.CONNECT;
        } else if (stmt.getObject().equals(WONMSG.TYPE_CONNECT_RESPONSE)) {
           type = WonMessageType.CONNECT_RESPONSE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_NEED_STATE)) {
          type = WonMessageType.NEED_STATE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_NEED_STATE_RESPONSE)) {
          type = WonMessageType.NEED_STATE_RESPONSE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_OPEN)) {
          type = WonMessageType.OPEN;
        } else if (stmt.getObject().equals(WONMSG.TYPE_OPEN_RESPONSE)) {
          type = WonMessageType.OPEN_RESPONSE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_CLOSE)) {
          type = WonMessageType.CLOSE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_CLOSE_RESPONSE)) {
          type = WonMessageType.CLOSE_RESPONSE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_CONNECTION_MESSAGE)) {
          type = WonMessageType.CONNECTION_MESSAGE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_CONNECTION_MESSAGE_RESPONSE)) {
          type = WonMessageType.CONNECTION_MESSAGE_RESPONSE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_CREATE_RESPONSE)) {
          type = WonMessageType.CREATE_RESPONSE;
        }
        // ToDo (FS): also check if the predicate is msg:hasResponseState
        else if (stmt.getObject().equals(WONMSG.TYPE_RESPONSE_STATE_SUCCESS)) {
          responseState = WONMSG.TYPE_RESPONSE_STATE_SUCCESS;
        } else if (stmt.getObject().equals(WONMSG.TYPE_RESPONSE_STATE_FAILURE)) {
          responseState = WONMSG.TYPE_RESPONSE_STATE_FAILURE;
        } else if (stmt.getObject().equals(WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_NEED_ID)) {
          responseState = WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_NEED_ID;
        } else if (stmt.getObject().equals(WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_CONNECTION_ID)) {
          responseState = WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_CONNECTION_ID;
        } else if (stmt.getObject().equals(WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_MESSAGE_ID)) {
          responseState = WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_MESSAGE_ID;
        }
        if (type != null) {
          eventRes = stmt.getSubject();
          break;
        }
      }
      msgEvent.setMessageType(type);
      msgEvent.setMessageURI(URI.create(eventRes.getURI()));
      msgEvent.setResponseState(URI.create(responseState.getURI()));

      stmtIterator = model.listStatements(eventRes, null, RdfUtils.EMPTY_RDF_NODE);
      while (stmtIterator.hasNext()) {
        Statement stmt = stmtIterator.nextStatement();
        Property pred = stmt.getPredicate();
        if (pred.equals(WONMSG.RECEIVER_PROPERTY)) {
          msgEvent.setReceiverURI(URI.create(stmt.getObject().asResource().getURI()));
        } else if (pred.equals(WONMSG.RECEIVER_NEED_PROPERTY)) {
          msgEvent.setReceiverNeedURI(URI.create(stmt.getObject().asResource().getURI()));
        } else if (pred.equals(WONMSG.RECEIVER_NODE_PROPERTY)) {
          msgEvent.setReceiverNodeURI(URI.create(stmt.getObject().asResource().getURI()));
        } else if (pred.equals(WONMSG.SENDER_PROPERTY)) {
          msgEvent.setSenderURI(URI.create(stmt.getObject().asResource().getURI()));
        } else if (pred.equals(WONMSG.SENDER_NEED_PROPERTY)) {
          msgEvent.setSenderNeedURI(URI.create(stmt.getObject().asResource().getURI()));
        } else if (pred.equals(WONMSG.SENDER_NODE_PROPERTY)) {
          msgEvent.setSenderNodeURI(URI.create(stmt.getObject().asResource().getURI()));
        } else if (pred.equals(WONMSG.NEW_NEED_STATE_PROPERTY)) {
          String stateName = stmt.getObject().asResource().getURI().substring(WON.BASE_URI.length());
          msgEvent.setNewNeedState(NeedState.parseString(stateName));
        } else if (pred.equals(WONMSG.HAS_CONTENT_PROPERTY)) {
          msgEvent.addHasContent(URI.create(stmt.getObject().asResource().getURI()));
        } else if (pred.equals(WONMSG.REFERS_TO_PROPERTY)) {
          msgEvent.addRefersTo(URI.create(stmt.getObject().asResource().getURI()));
        }
      }

      // get the signatures that the message event contains (if present)
      addSignatures(msgEvent.getHasContent(), msgEvent, model);
      addSignatures(msgEvent.getRefersTo(), msgEvent, model);

// maybe separate processing for each message type makes sense...
//      switch (type) {
//        case CREATE_NEED:
//          addPropertiesOfCreateNeedMessage(eventRes, msgEvent, model);
//          break;
//        case CONNECT:
//          addPropertiesOfConnectMessage(eventRes, msgEvent, model);
//          break;
//        case NEED_STATE:
//          addPropertiesOfNeedStateMessage(eventRes, msgEvent, model);
//          break;
//        default:
//          throw new IllegalArgumentException("Mapping not supported from message type " + type.toString());
//          break;
//      }

      return msgEvent;
    }

    // TODO to be replaced by implementation in crypto package:
    // should be own reader/writer for signature that should be
    // called here
    private void addSignatures(final List<URI> graphURIs, final MessageEvent msgEvent, final Model model) {
      for (URI uri : graphURIs) {
        Resource resource = model.createResource(uri.toString());
        // TODO
      }
    }


  }
