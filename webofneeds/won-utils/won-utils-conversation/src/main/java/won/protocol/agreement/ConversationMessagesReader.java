package won.protocol.agreement;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

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
        inithandlers.put(WONMSG.senderAtom,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .setSenderAtomURI(getUri(s.getObject())));
        inithandlers.put(WONMSG.correspondingRemoteMessage,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .setCorrespondingRemoteMessageURI(getUri(s.getObject())));
        inithandlers.put(WONMSG.forwardedMessage,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addForwarded(getUri(s.getObject())));
        inithandlers.put(WONMSG.previousMessage,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addPrevious(getUri(s.getObject())));
        inithandlers.put(WONMSG.isResponseTo,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .setIsResponseTo(getUri(s.getObject())));
        inithandlers.put(WONMSG.isRemoteResponseTo,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .setIsRemoteResponseTo(getUri(s.getObject())));
        inithandlers.put(WONMSG.messageType, (Map<URI, ConversationMessage> messages, Statement s) -> {
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
        inithandlers.put(WONMSG.content,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addContentGraph(getUri(s.getObject())));
        inithandlers.put(WONMOD.retracts,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addRetracts(getUri(s.getObject())));
        inithandlers.put(WONAGR.proposes,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addProposes(getUri(s.getObject())));
        inithandlers.put(WONAGR.claims,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addClaims(getUri(s.getObject())));
        inithandlers.put(WONAGR.proposesToCancel,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addProposesToCancel(getUri(s.getObject())));
        inithandlers.put(WONAGR.rejects,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addRejects(getUri(s.getObject())));
        inithandlers.put(WONAGR.accepts,
                        (Map<URI, ConversationMessage> messages,
                                        Statement s) -> getOrCreateMessage(messages, getUri(s.getSubject()))
                                                        .addAccepts(getUri(s.getObject())));
        handlers = Collections.unmodifiableMap(inithandlers);
    }
}
