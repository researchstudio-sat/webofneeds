package won.protocol.agreement;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WONAGR;
import won.protocol.vocabulary.WONMOD;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ConversationMessagesReader {

  static final Map<Property, BiConsumer<Map<URI, ConversationMessage>, Statement>> handlers;

  public static Map<URI, ConversationMessage> readConversationMessages(Dataset dataset) {
    Map<URI, ConversationMessage> messages = new HashMap<>();
    RdfUtils.toStatementStream(dataset).forEach(stmt -> {
      BiConsumer handler = handlers.get(stmt.getPredicate());
      if (handler != null) {
        handler.accept(messages, stmt);
      }
    });
    return messages;
  }

  private static URI getUri(RDFNode node) {
    if (!node.isResource() || node.isAnon())
      return null;
    return URI.create(node.asResource().getURI());
  }

  private static ConversationMessage getOrCreateMessage(Map<URI, ConversationMessage> messages, URI uri) {
    ConversationMessage msg = messages.get(uri);
    if (msg == null) {
      msg = new ConversationMessage(uri);
      messages.put(uri, msg);
    }
    return msg;
  }

  static {

    Map<Property, BiConsumer<Map<URI, ConversationMessage>, Statement>> inithandlers = new HashMap<>();

    inithandlers.put(WONMSG.SENDER_NEED_PROPERTY,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .setSenderNeedURI(getUri(s.getObject())));

    inithandlers.put(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .setCorrespondingRemoteMessageURI(getUri(s.getObject())));

    inithandlers.put(WONMSG.HAS_FORWARDED_MESSAGE,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addForwarded(getUri(s.getObject())));

    inithandlers.put(WONMSG.HAS_PREVIOUS_MESSAGE_PROPERTY,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addPrevious(getUri(s.getObject())));

    inithandlers.put(WONMSG.IS_RESPONSE_TO,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .setIsResponseTo(getUri(s.getObject())));

    inithandlers.put(WONMSG.IS_REMOTE_RESPONSE_TO,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .setIsRemoteResponseTo(getUri(s.getObject())));

    inithandlers.put(WONMSG.HAS_MESSAGE_TYPE_PROPERTY, (Map<URI, ConversationMessage> messages, Statement s) -> {
      URI uri = getUri(s.getObject());
      if (uri == null) {
        return;
      }
      WonMessageType type = WonMessageType.getWonMessageType(uri);
      if (type == null) {
        return;
      }
      getOrCreateMessage(messages, getUri(s.getSubject())).setMessageType(type);
    });

    inithandlers.put(RDF.type, (Map<URI, ConversationMessage> messages, Statement s) -> {

      URI uri = getUri(s.getObject());
      if (uri == null) {
        return;
      }
      WonMessageDirection direction = WonMessageDirection.getWonMessageDirection(uri);
      if (direction == null) {
        return;
      }
      getOrCreateMessage(messages, getUri(s.getSubject())).setDirection(direction);
    });

    inithandlers.put(WONMSG.HAS_CONTENT_PROPERTY,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addContentGraph(getUri(s.getObject())));

    inithandlers.put(WONMOD.RETRACTS,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addRetracts(getUri(s.getObject())));

    inithandlers.put(WONAGR.PROPOSES,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addProposes(getUri(s.getObject())));

    inithandlers.put(WONAGR.CLAIMS,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addClaims(getUri(s.getObject())));

    inithandlers.put(WONAGR.PROPOSES_TO_CANCEL,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addProposesToCancel(getUri(s.getObject())));

    inithandlers.put(WONAGR.REJECTS,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addRejects(getUri(s.getObject())));

    inithandlers.put(WONAGR.ACCEPTS,
        (Map<URI, ConversationMessage> messages, Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
            .addAccepts(getUri(s.getObject())));

    handlers = Collections.unmodifiableMap(inithandlers);
  }
}
