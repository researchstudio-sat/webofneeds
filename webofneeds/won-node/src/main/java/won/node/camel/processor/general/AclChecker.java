package won.node.camel.processor.general;

import org.apache.camel.Exchange;
import org.apache.jena.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.auth.AuthUtils;
import won.auth.WonAclEvaluator;
import won.auth.WonAclEvaluatorFactory;
import won.auth.model.AclEvalResult;
import won.auth.model.DecisionValue;
import won.auth.model.MessageOperationExpression;
import won.auth.model.OperationRequest;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.service.WonCamelHelper;
import won.protocol.exception.ForbiddenMessageException;
import won.protocol.exception.IllegalMessageSignerException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.Socket;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import static won.auth.model.Individuals.*;

public class AclChecker extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private WonAclEvaluatorFactory wonAclEvaluatorFactory;

    @Override
    public void process(Exchange exchange) throws Exception {
        URI recipientAtomUri = WonCamelHelper.getRecipientAtomURIRequired(exchange);
        WonMessage message = WonCamelHelper.getMessageRequired(exchange);
        if (message.getMessageType().isCreateAtom()) {
            // no checks required for a create message
            return;
        }
        if (message.getMessageType().isHintMessage()) {
            // no checks required for a hint message
            return;
        }
        URI signer = message.getSignerURIRequired();
        URI sender = message.getSenderAtomURIRequired();
        URI senderNode = message.getSenderNodeURIRequired();
        WonMessageDirection direction = WonCamelHelper.getDirectionRequired(exchange);
        if (direction.isFromExternal() || direction.isFromSystem()) {
            if (signer.equals(senderNode)) {
                // nodes are allowed to messages on behalf of an atom (for now)
                return;
            }
        }
        Atom atom = atomService.getAtomForMessageRequired(message, direction);
        Optional<Graph> aclGraph = atom.getAclGraph();
        if (aclGraph.isEmpty()) {
            // no checks if no acl present
            if (!sender.equals(signer)) {
                throw new IllegalMessageSignerException(
                                String.format("%s must not be signer of %s message %s",
                                                signer,
                                                message.getMessageTypeRequired(),
                                                message.getMessageURIRequired()));
            }
            return;
        }
        boolean isMessageOnBehalf = false;
        URI requestor = sender;
        if (direction.isFromOwner()) {
            if (signer.equals(sender)) {
                // no checks if the owner is the signer
                return;
            } else {
                isMessageOnBehalf = true;
                requestor = signer;
            }
        }
        OperationRequest operationRequest = OperationRequest.builder()
                        .setReqAtom(atom.getAtomURI())
                        .setReqAtomState(AuthUtils.toAuthAtomState(atom.getState()))
                        .setRequestor(requestor)
                        .build();
        if (message.getMessageType().isConnectionSpecificMessage()) {
            if (direction.isFromOwner() || direction.isFromSystem()) {
                Optional<Socket> s = socketService.getSocket(message.getSenderSocketURIRequired());
                operationRequest.setReqSocketType(s.get().getTypeURI());
                operationRequest.setReqSocket(s.get().getSocketURI());
            } else if (direction.isFromExternal()) {
                Optional<Socket> s = socketService.getSocket(message.getRecipientSocketURIRequired());
                operationRequest.setReqSocketType(s.get().getTypeURI());
                operationRequest.setReqSocket(s.get().getSocketURI());
            }
        } else {
            operationRequest.setReqPosition(POSITION_ROOT);
        }
        Optional<Connection> con = WonCamelHelper.getConnection(exchange, connectionService);
        if (con.isPresent()) {
            operationRequest.setReqPosition(POSITION_CONNECTION);
            Connection c = con.get();
            operationRequest.setReqConnection(c.getConnectionURI());
            operationRequest.setReqConnectionTargetAtom(c.getTargetAtomURI());
            operationRequest.setReqConnectionState(AuthUtils.toAuthConnectionState(c.getState()));
            // we already set these values, but now that we know we have a connection, make
            // sure these values match the connection's
            operationRequest.setReqSocketType(c.getTypeURI());
            operationRequest.setReqSocket(c.getSocketURI());
        } else {
            operationRequest.setReqPosition(POSITION_SOCKET);
        }
        if (isMessageOnBehalf) {
            operationRequest.setOperationMessageOperationExpression(
                            MessageOperationExpression.builder()
                                            .addMessageOnBehalfsMessageType(
                                                            AuthUtils.toAuthMessageType(message.getMessageType()))
                                            .build());
        } else {
            operationRequest.setOperationMessageOperationExpression(
                            MessageOperationExpression.builder()
                                            .addMessageTosMessageType(
                                                            AuthUtils.toAuthMessageType(message.getMessageType()))
                                            .build());
        }
        WonAclEvaluator evaluator = wonAclEvaluatorFactory.create(aclGraph.get());
        if (message.getMessageType().isReplace()) {
            // decision for replace messages is deferred to where replacement actually
            // happens
        } else {
            AclEvalResult result = evaluator.decide(operationRequest);
            if (DecisionValue.ACCESS_DENIED.equals(result.getDecision())) {
                throw new ForbiddenMessageException(
                                String.format("Message not allowed"));
            }
        }
        WonCamelHelper.putWonAclEvaluator(exchange, evaluator);
        WonCamelHelper.putWonAclOperationRequest(exchange, operationRequest);
    }
}
