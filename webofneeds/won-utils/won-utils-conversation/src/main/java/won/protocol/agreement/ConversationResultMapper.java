package won.protocol.agreement;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;

import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;

public class ConversationResultMapper implements Function<QuerySolution, ConversationMessage> {
    private Map<URI, ConversationMessage> knownMessages = null;

    public ConversationResultMapper(Map messages) {
        this.knownMessages = messages;
    }

    public Map<URI, ConversationMessage> getKnownMessages() {
        return this.knownMessages;
    }

    @Override
    public ConversationMessage apply(QuerySolution solution) {
        URI messageUri = getUriVar(solution, "msg");
        ConversationMessage ret = knownMessages.get(messageUri);
        if (ret == null) {
            ret = new ConversationMessage(messageUri);
        }
        URI senderNeedUri = getUriVar(solution, "senderNeed");
        if (senderNeedUri != null) {
            ret.setSenderNeedURI(senderNeedUri);
        }
        URI previous = getUriVar(solution, "previous");
        if (previous != null) {
            ret.addPrevious(previous);
        }
        URI retracts = getUriVar(solution, "retracts");
        if (retracts != null) {
            ret.addRetracts(retracts);
        }
        URI proposes = getUriVar(solution, "proposes");
        if (proposes != null) {
            ret.addProposes(proposes);
        }
        URI rejects = getUriVar(solution, "rejects");
        if (rejects != null) {
            ret.addRejects(rejects);
        }
        URI proposesToCancel = getUriVar(solution, "proposesToCancel");
        if (proposesToCancel != null) {
            ret.addProposesToCancel(proposesToCancel);
        }
        URI accepts = getUriVar(solution, "accepts");
        if (accepts != null) {
            ret.addAccepts(accepts);
        }
        URI correspondingRemoteMessage = getUriVar(solution, "correspondingRemoteMessage");
        if (correspondingRemoteMessage != null) {
            ret.setCorrespondingRemoteMessageURI(correspondingRemoteMessage);
        }
        URI isResponseTo = getUriVar(solution, "isResponseTo");
        if (isResponseTo != null) {
            ret.setIsResponseTo(isResponseTo);
        }
        URI isRemoteResponseTo = getUriVar(solution, "isRemoteResponseTo");
        if (isRemoteResponseTo != null) {
            ret.setIsRemoteResponseTo(isRemoteResponseTo);
        }
        URI type = getUriVar(solution, "msgType");
        if (type != null) {
            ret.setMessageType(WonMessageType.getWonMessageType(type));
        }
        URI direction = getUriVar(solution, "direction");
        if (direction != null) {
            ret.setDirection(WonMessageDirection.getWonMessageDirection(direction));
        }
        URI contentGraph = getUriVar(solution, "contentGraph");
        if (contentGraph != null) {
            ret.addContentGraph(contentGraph);
        }
        this.knownMessages.put(messageUri, ret);
        return ret;
    }

    private URI getUriVar(QuerySolution solution, String name) {
        RDFNode node = solution.get(name);
        if (node == null) {
            return null;
        }
        return URI.create(node.asResource().getURI());
    }
}