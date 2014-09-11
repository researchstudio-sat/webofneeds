package won.protocol.message;

import com.hp.hpl.jena.rdf.model.*;
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
      messageResource.addProperty(WONMSG.HAS_MESSAGE_TYPE_PROPERTY, messageEvent.getMessageType().getResource());

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

//      // create signatures
//      for (URI uriOfSignedGraph : messageEvent.getSignatures().keySet()) {
//        // add signature model triples to event triples
//        RdfUtils.addAllStatements(model, messageEvent.getSignatures().get(uriOfSignedGraph));
//        RdfUtils.addPrefixMapping(model, messageEvent.getSignatures().get(uriOfSignedGraph));
//      }

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
      StmtIterator stmtIterator = model.listStatements(null, WONMSG.HAS_MESSAGE_TYPE_PROPERTY, RdfUtils.EMPTY_RDF_NODE);
      while (stmtIterator.hasNext()) {
        Statement stmt = stmtIterator.nextStatement();
        type = WonMessageType.getWonMessageType(stmt.getObject().asResource());
        if (type != null) {
          eventRes = stmt.getSubject();
          break;
        }
      }
      msgEvent.setMessageType(type);
      msgEvent.setMessageURI(URI.create(eventRes.getURI()));

      ResponseState responseState = null;
      stmtIterator =
        model.listStatements(null, WONMSG.HAS_RESPONSE_STATE_PROPERTY, RdfUtils.EMPTY_RDF_NODE);
      while (stmtIterator.hasNext()) {
        Statement stmt = stmtIterator.nextStatement();

        responseState = ResponseState.getResponseState(stmt.getObject().asResource());
        if (responseState != null) {
          break;
        }
      }

      if(responseState != null){
        msgEvent.setResponseState(URI.create(responseState.getResource().getURI()));
      }

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
