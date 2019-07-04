/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.owner.web.websocket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import won.owner.model.User;
import won.owner.model.UserAtom;
import won.owner.repository.UserAtomRepository;
import won.owner.repository.UserRepository;
import won.owner.service.impl.KeystoreEnabledUserDetails;
import won.owner.service.impl.OwnerApplicationService;
import won.owner.web.WonOwnerMailSender;
import won.owner.web.WonOwnerPushSender;
import won.owner.web.service.ServerSideActionService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.model.AtomState;
import won.protocol.util.AuthenticationThreadLocal;
import won.protocol.util.LoggingUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * User: syim Date: 06.08.14
 */
public class WonWebSocketHandler extends TextWebSocketHandler implements WonMessageProcessor, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // if we're receiving a partial message, a StringBuilder will be in the
    // session's attributes map under this key
    private static final String SESSION_ATTRIBUTE_PARTIAL_MESSAGE = "partialMessage";
    private OwnerApplicationService ownerApplicationService;
    @Autowired
    private WebSocketSessionService webSocketSessionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserAtomRepository userAtomRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    private WonOwnerMailSender emailSender;
    @Autowired
    private WonOwnerPushSender pushSender;
    @Autowired
    private EagerlyCachePopulatingMessageProcessor eagerlyCachePopulatingProcessor;
    @Autowired
    private ServerSideActionService serverSideActionService;
    @Autowired
    private LinkedDataSource linkedDataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.ownerApplicationService.setMessageProcessorDelegate(this);
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        // remember which user or (if not logged in) which atomUri the session is bound
        // to
        User user = getUserForSession(session);
        if (user != null) {
            logger.debug("connection established, binding session to user {}", user.getId());
            this.webSocketSessionService.addMapping(user, session);
        } else {
            logger.debug("connection established, but no user found in session to bind to");
        }
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        User user = getUserForSession(session);
        if (user != null) {
            logger.debug("session closed, removing session bindings to user {}", user.getId());
            this.webSocketSessionService.removeMapping(user, session);
            for (UserAtom userAtom : user.getUserAtoms()) {
                logger.debug("removing session bindings to atom {}", userAtom.getUri());
                this.webSocketSessionService.removeMapping(userAtom.getUri(), session);
            }
        } else {
            logger.debug("connection closed, but no user found in session, no bindings removed");
        }
    }

    /*
     * User user = getCurrentUser(); logger.info("New Atom:" +
     * atomPojo.getTextDescription() + "/" + atomPojo.getCreationDate() + "/" +
     * atomPojo.getLongitude() + "/" + atomPojo.getLatitude() + "/" +
     * (atomPojo.getState() == AtomState.ACTIVE)); //TODO: using fixed Sockets -
     * change this atomPojo.setSocketTypes(new String[]{
     * SocketType.ChatSocket.getURI().toString()}); AtomPojo createdAtomPojo =
     * resolve(atomPojo); Atom atom =
     * atomRepository.findOne(createdAtomPojo.getAtomId());
     * user.getAtoms().add(atom); wonUserDetailService.save(user); HttpHeaders
     * headers = new HttpHeaders(); headers.setLocation(atom.getAtomURI()); return
     * new ResponseEntity<AtomPojo>(createdAtomPojo, headers, HttpStatus.CREATED);
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS, isolation = Isolation.READ_COMMITTED)
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        logger.debug("OA Server - WebSocket message received: {}", message.getPayload());
        updateSession(session);
        if (!message.isLast()) {
            // we have an intermediate part of the current message.
            session.getAttributes().putIfAbsent(SESSION_ATTRIBUTE_PARTIAL_MESSAGE, new StringBuilder());
        }
        // now check if we have the partial message string builder in the session.
        // if we do, we're processing a partial message, and we have to append the
        // current message payload
        StringBuilder sb = (StringBuilder) session.getAttributes().get(SESSION_ATTRIBUTE_PARTIAL_MESSAGE);
        String completePayload = null; // will hold the final message
        if (sb == null) {
            // No string builder found in the session - we're not processing a partial
            // message.
            // The complete payload is in the current message. Get it and continue.
            completePayload = message.getPayload();
        } else {
            // the string builder is there - we're processing a partial message. append the
            // current piece
            sb.append(message.getPayload());
            if (message.isLast()) {
                // we've received the last part. pass it on to the next processing steps.
                completePayload = sb.toString();
                // also, we do not atom the string builder in the session any longer. remove it:
                session.getAttributes().remove(SESSION_ATTRIBUTE_PARTIAL_MESSAGE);
            } else {
                // This is not the last part of the message.
                // We have stored it along with all previous parts. Abort processing this
                // message and wait for the
                // next part
                return;
            }
        }
        final String ECHO_STRING = "e";
        if (completePayload.equals(ECHO_STRING)) {
            return;
        }
        WonMessage wonMessage = null;
        URI atomUri = null;
        try {
            wonMessage = WonMessageDecoder.decodeFromJsonLd(completePayload);
            // remember which user or (if not logged in) which atomUri the session is bound
            // to
            User user = getUserForSession(session);
            if (user != null) {
                logger.debug("binding session to user {}", user.getId());
                this.webSocketSessionService.addMapping(user, session);
            }
            // anyway, we have to bind the URI to the session, otherwise we can't handle
            // incoming server->client messages
            atomUri = wonMessage.getSenderAtomURI();
            logger.debug("binding session to atom URI {}", atomUri);
            this.webSocketSessionService.addMapping(atomUri, session);
        } catch (Exception e) {
            // ignore this message
            LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e,
                            "Ignoring WonMessage received via Websocket that caused an Exception");
            WebSocketMessage<String> wsMsg = new TextMessage(
                            "{'error':'Error processing WonMessage: " + e.getMessage() + "'}");
            return;
        }
        try {
            AuthenticationThreadLocal.setAuthentication((Authentication) session.getPrincipal());
            ownerApplicationService.sendWonMessage(wonMessage);
            if (wonMessage.getMessageType() == WonMessageType.DELETE) {
                // TODO: Set in STATE "inDeletion" and delete after it's deleted in the node
                // (receiving success response for delete msg)
                try {
                    // Get the atom from owner application db
                    UserAtom userAtom = userAtomRepository.findByAtomUri(atomUri);
                    userAtom.setState(AtomState.DELETED);
                    userAtomRepository.save(userAtom);
                    /*
                     * //Get the user from owner application db user =
                     * userRepository.findOne(user.getId()); //Delete atom in users atom list and
                     * save changes user.deleteAtomUri(userAtom); userRepository.save(user);
                     * //Delete atom in atom repository userAtomRepository.delete(userAtom.getId());
                     */
                } catch (Exception e) {
                    logger.debug("Could not delete atom with  uri {} because of {}", atomUri, e);
                }
            }
        } finally {
            // be sure to remove the principal from the threadlocal
            AuthenticationThreadLocal.remove();
        }
    }

    /*
     * update the session last accessed time, - spring-session was added to
     * synchronize // http sessions with websocket session // see:
     * http://spring.io/blog/2014/09/16/preview-spring-security-websocket-support-
     * sessions // Currently used here Sping's MapSession implementation of Spring
     * has default timeout of 30 minutes
     */
    private void updateSession(final WebSocketSession session) {
        String sessionId = (String) session.getAttributes().get(WonHandshakeInterceptor.SESSION_ATTR);
        if (sessionId != null) {
            Session activeSession = sessionRepository.getSession(sessionId);
            if (activeSession != null) {
                sessionRepository.save(activeSession);
            }
        }
    }

    /**
     * We want to keep the buffer in the underlying server small (8k per websocket),
     * but still be able to receive large messages. Hence, we have to be able to
     * handle partial messages here.
     */
    @Override
    public boolean supportsPartialMessages() {
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public WonMessage process(final WonMessage wonMessage) {
        try {
            String wonMessageJsonLdString = WonMessageEncoder.encodeAsJsonLd(wonMessage);
            WebSocketMessage<String> webSocketMessage = new TextMessage(wonMessageJsonLdString);
            URI atomUri = getOwnedAtomURI(wonMessage);
            User user = getUserForWonMessage(wonMessage);
            Set<WebSocketSession> webSocketSessions = findWebSocketSessionsForWonMessage(wonMessage, atomUri, user);
            // check if we can deliver the message. If not, send email.
            if (webSocketSessions.size() == 0) {
                logger.debug("cannot deliver message of type {} for atom {}, receiver {}: no websocket session found. Trying to send message by email.",
                                new Object[] { wonMessage.getMessageType(), wonMessage.getRecipientAtomURI(),
                                                wonMessage.getRecipientURI() });
                // send per email notifications if it applies:
                notifyPerEmail(user, atomUri, wonMessage);
                return wonMessage;
            }
            notifyPerPush(user, atomUri, wonMessage);
            // we can send it - pre-cache the delivery chain:
            eagerlyCachePopulatingProcessor.process(wonMessage);
            // send to owner webapp
            int successfullySent = 0;
            for (WebSocketSession session : webSocketSessions) {
                successfullySent += sendMessageForSession(wonMessage, webSocketMessage, session, atomUri, user) ? 1 : 0;
            }
            if (successfullySent == 0) {
                // we did not manage to send the message via the websocket, send it by email.
                logger.debug("cannot deliver message of type {} for atom {}, receiver {}: none of the associated websocket sessions worked. Trying to send message by webpush and email.",
                                new Object[] { wonMessage.getMessageType(), wonMessage.getRecipientAtomURI(),
                                                wonMessage.getRecipientURI() });
                // TODO: ideally in this case
                // 1. collect multiple events occurring in close succession
                // 2. try to push
                // 3. email only if push was not successful
                notifyPerEmail(user, atomUri, wonMessage);
            }
            return wonMessage;
        } finally {
            // in any case, let the serversideactionservice do its work, if there is any to
            // do:
            serverSideActionService.process(wonMessage);
        }
    }

    private void notifyPerPush(final User user, final URI atomUri, final WonMessage wonMessage) {
        if (wonMessage.getEnvelopeType() == WonMessageDirection.FROM_OWNER) {
            // we assume that this message, coming from the server here, can only be an
            // echoed message. don't send by email.
            logger.debug("not sending notification to user: message {} looks like an echo from the server",
                            wonMessage.getMessageURI());
            return;
        }
        if (user == null) {
            logger.info("not sending notification to user: user not specified");
            return;
        }
        UserAtom userAtom = getAtomOfUser(user, atomUri);
        if (userAtom == null) {
            logger.debug("not sending notification to user: atom uri not specified");
            return;
        }
        String textMsg = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        switch (wonMessage.getMessageType()) {
            case CONNECTION_MESSAGE:
            case OPEN:
                if (userAtom.isConversations()) {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode rootNode = mapper.createObjectNode();
                    rootNode.put("type", "MESSAGE");
                    rootNode.put("atomUri", userAtom.getUri().toString());
                    rootNode.put("connectionUri", wonMessage.getRecipientURI().toString());
                    rootNode.put("message", textMsg);
                    String stringifiedJson;
                    try {
                        stringifiedJson = mapper.writer().writeValueAsString(rootNode);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    pushSender.sendNotification(user, stringifiedJson);
                }
                return;
            case SOCKET_HINT_MESSAGE:
                if (userAtom.isMatches()) {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode rootNode = mapper.createObjectNode();
                    rootNode.put("type", "HINT");
                    rootNode.put("atomUri", userAtom.getUri().toString());
                    rootNode.put("connectionUri", wonMessage.getRecipientURI().toString());
                    String stringifiedJson;
                    try {
                        stringifiedJson = mapper.writer().writeValueAsString(rootNode);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    pushSender.sendNotification(user, stringifiedJson);
                }
                return;
            case CONNECT:
                if (userAtom.isRequests()) {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode rootNode = mapper.createObjectNode();
                    rootNode.put("type", "CONNECT");
                    rootNode.put("atomUri", userAtom.getUri().toString());
                    rootNode.put("connectionUri", wonMessage.getRecipientURI().toString());
                    rootNode.put("message", textMsg);
                    String stringifiedJson;
                    try {
                        stringifiedJson = mapper.writer().writeValueAsString(rootNode);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    pushSender.sendNotification(user, stringifiedJson);
                }
                return;
            default:
                return;
        }
    }

    private void notifyPerEmail(final User user, final URI atomUri, final WonMessage wonMessage) {
        if (wonMessage.getEnvelopeType() == WonMessageDirection.FROM_OWNER) {
            // we assume that this message, coming from the server here, can only be an
            // echoed message. don't send by email.
            logger.debug("not sending email to user: message {} looks like an echo from the server",
                            wonMessage.getMessageURI());
            return;
        }
        if (user == null) {
            logger.info("not sending email to user: user not specified");
            return;
        }
        if (user.isAnonymous()) {
            logger.debug("not sending email to user: user is anonymous");
            return;
        }
        if (!user.isEmailVerified()) {
            logger.debug("not sending email to user: email address not yet verified");
            return;
        }
        UserAtom userAtom = getAtomOfUser(user, atomUri);
        if (userAtom == null) {
            logger.debug("not sending email to user: atom uri not specified");
            return;
        }
        String textMsg = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        try {
            switch (wonMessage.getMessageType()) {
                case OPEN:
                    if (userAtom.isConversations()) {
                        emailSender.sendConversationNotificationMessage(user.getEmail(), atomUri.toString(),
                                        wonMessage.getSenderAtomURI().toString(),
                                        wonMessage.getRecipientURI().toString(), textMsg);
                    }
                    return;
                case CONNECTION_MESSAGE:
                    if (userAtom.isConversations()) {
                        emailSender.sendConversationNotificationMessage(user.getEmail(), atomUri.toString(),
                                        wonMessage.getSenderAtomURI().toString(),
                                        wonMessage.getRecipientURI().toString(), textMsg);
                    }
                    return;
                case CONNECT:
                    if (userAtom.isRequests()) {
                        emailSender.sendConnectNotificationMessage(user.getEmail(), atomUri.toString(),
                                        wonMessage.getSenderAtomURI().toString(),
                                        wonMessage.getRecipientURI().toString(), textMsg);
                    }
                    return;
                case ATOM_HINT_MESSAGE:
                    if (userAtom.isMatches()) {
                        String targetAtomUri = wonMessage.getHintTargetAtomURI().toString();
                        emailSender.sendHintNotificationMessage(user.getEmail(), atomUri.toString(), targetAtomUri,
                                        wonMessage.getRecipientURI().toString());
                    }
                    return;
                case SOCKET_HINT_MESSAGE:
                    if (userAtom.isMatches()) {
                        Optional<URI> targetAtomUri = WonLinkedDataUtils
                                        .getAtomOfSocket(wonMessage.getHintTargetSocketURI(), linkedDataSource);
                        if (targetAtomUri.isPresent()) {
                            emailSender.sendHintNotificationMessage(user.getEmail(), atomUri.toString(),
                                            targetAtomUri.get().toString(), wonMessage.getRecipientURI().toString());
                        } else {
                            logger.info("received socket hint to {} but could not identify corresponding atom - no mail sent.",
                                            wonMessage.getHintTargetSocketURI());
                        }
                    }
                    return;
                case CLOSE:
                    // do not send emails for a close
                    return;
                case DEACTIVATE:
                    // a deactivate message, coming from the WoN node. Always deliverd by email.
                    emailSender.sendSystemDeactivateNotificationMessage(user.getEmail(), atomUri.toString(), textMsg);
                    return;
                case ATOM_MESSAGE:
                    // an atom message, coming from the WoN node. Always deliverd by email.
                    emailSender.sendAtomMessageNotificationMessage(user.getEmail(), atomUri.toString(), textMsg);
                    return;
                default:
                    return;
            }
        } catch (MailException ex) { // org.springframework.mail.MailException
            logger.error("Email could not be sent", ex);
        }
    }

    private Set<WebSocketSession> findWebSocketSessionsForWonMessage(final WonMessage wonMessage, URI atomUri,
                    User user) {
        assert wonMessage != null : "wonMessage must not be null";
        assert atomUri != null : "atomUri must not be null";
        Set<WebSocketSession> webSocketSessions = webSocketSessionService.getWebSocketSessions(atomUri);
        if (webSocketSessions == null)
            webSocketSessions = new HashSet();
        logger.debug("found {} sessions for atom uri {}, now removing closed sessions", webSocketSessions.size(),
                        atomUri);
        removeClosedSessions(webSocketSessions, atomUri);
        if (user != null) {
            Set<WebSocketSession> userSessions = webSocketSessionService.getWebSocketSessions(user);
            if (userSessions == null)
                userSessions = new HashSet();
            logger.debug("found {} sessions for user {}, now removing closed sessions", userSessions.size(),
                            user.getId());
            removeClosedSessions(userSessions, user);
            webSocketSessions.addAll(userSessions);
        }
        return webSocketSessions;
    }

    private void removeClosedSessions(final Set<WebSocketSession> webSocketSessions, final URI atomUri) {
        for (Iterator<WebSocketSession> it = webSocketSessions.iterator(); it.hasNext();) {
            WebSocketSession session = it.next();
            if (!session.isOpen()) {
                logger.debug("removing closed websocket session {} of atom {}", session.getId(), atomUri);
                webSocketSessionService.removeMapping(atomUri, session);
                it.remove();
            }
        }
    }

    private void removeClosedSessions(final Set<WebSocketSession> webSocketSessions, final User user) {
        for (Iterator<WebSocketSession> it = webSocketSessions.iterator(); it.hasNext();) {
            WebSocketSession session = it.next();
            if (!session.isOpen()) {
                logger.debug("removing closed websocket session {} of user {}", session.getId(), user.getId());
                webSocketSessionService.removeMapping(user, session);
                it.remove();
            }
        }
    }

    private User getUserForWonMessage(final WonMessage wonMessage) {
        URI atomUri = getOwnedAtomURI(wonMessage);
        return userRepository.findByAtomUri(atomUri);
    }

    private UserAtom getAtomOfUser(final User user, final URI atomUri) {
        for (UserAtom userAtom : user.getUserAtoms()) {
            if (userAtom.getUri().equals(atomUri)) {
                return userAtom;
            }
        }
        return null;
    }

    /**
     * Sends the specified message over the socket unless it is a successresponse to
     * create, deactivate, activate.
     * 
     * @param wonMessage
     * @param webSocketMessage
     * @param session
     * @param atomUri
     * @param user
     * @return true if a message was sent (or suppressed as planned), false if
     * something went wrong and the message could not be sent
     */
    private synchronized boolean sendMessageForSession(final WonMessage wonMessage,
                    final WebSocketMessage<String> webSocketMessage, final WebSocketSession session, URI atomUri,
                    User user) {
        if (!session.isOpen()) {
            logger.debug("session {} is closed, can't send message", session.getId());
            return false;
        }
        if (wonMessage.getMessageType() == WonMessageType.SUCCESS_RESPONSE) {
            if (WonMessageType.CREATE_ATOM == wonMessage.getIsResponseToMessageType()) {
                try {
                    saveAtomUriWithUser(wonMessage, session);
                } catch (Exception e) {
                    logger.warn("could not associate atom {} with currently logged in user, cannot send message",
                                    atomUri, e);
                    if (user != null) {
                        webSocketSessionService.removeMapping(user, session);
                    }
                    if (atomUri != null) {
                        webSocketSessionService.removeMapping(atomUri, session);
                    }
                    return true;
                }
            } else if (WonMessageType.DEACTIVATE == wonMessage.getIsResponseToMessageType()) {
                try {
                    deactivateAtomUri(wonMessage, session);
                } catch (Exception e) {
                    logger.warn("could not deactivate atom {}, cannot send message", atomUri, e);
                    if (user != null) {
                        webSocketSessionService.removeMapping(user, session);
                    }
                    if (atomUri != null) {
                        webSocketSessionService.removeMapping(atomUri, session);
                    }
                    return true;
                }
            } else if (WonMessageType.ACTIVATE == wonMessage.getIsResponseToMessageType()) {
                try {
                    activateAtomUri(wonMessage, session);
                } catch (Exception e) {
                    logger.warn("could not activate atom {}, cannot send message", atomUri, e);
                    if (user != null) {
                        webSocketSessionService.removeMapping(user, session);
                    }
                    if (atomUri != null) {
                        webSocketSessionService.removeMapping(atomUri, session);
                    }
                    return true;
                }
            } else if (WonMessageType.DELETE == wonMessage.getIsResponseToMessageType()) {
                try {
                    deleteAtomUri(wonMessage, session);
                } catch (Exception e) {
                    logger.warn("could not deactivate atom {}, cannot send message", atomUri, e);
                    if (user != null) {
                        webSocketSessionService.removeMapping(user, session);
                    }
                    if (atomUri != null) {
                        webSocketSessionService.removeMapping(atomUri, session);
                    }
                    return true;
                }
            }
        }
        try {
            logger.debug("OA Server - sending WebSocket message: {}", webSocketMessage);
            session.sendMessage(webSocketMessage);
        } catch (Exception e) {
            logger.warn(MessageFormat.format(
                            "caught exception while trying to send on session {1} for atomUri {2}, " + "user {3}",
                            session.getId(), atomUri, user), e);
            if (user != null) {
                webSocketSessionService.removeMapping(user, session);
            }
            if (atomUri != null) {
                webSocketSessionService.removeMapping(atomUri, session);
            }
            return false;
        }
        return true;
    }

    private void saveAtomUriWithUser(final WonMessage wonMessage, final WebSocketSession session) {
        User user = getUserForSession(session);
        URI atomUri = getOwnedAtomURI(wonMessage);
        UserAtom userAtom = new UserAtom(atomUri);
        // reload the user so we can save it
        // (the user object we get from getUserForSession is detached)
        user = userRepository.findOne(user.getId());
        userAtomRepository.save(userAtom);
        user.addAtomUri(userAtom);
        userRepository.save(user);
    }

    private void deactivateAtomUri(final WonMessage wonMessage, final WebSocketSession session) {
        updateAtomUriState(wonMessage, session, AtomState.INACTIVE);
    }

    private void activateAtomUri(final WonMessage wonMessage, final WebSocketSession session) {
        updateAtomUriState(wonMessage, session, AtomState.ACTIVE);
    }

    private void updateAtomUriState(final WonMessage wonMessage, final WebSocketSession session, AtomState newState) {
        URI atomUri = getOwnedAtomURI(wonMessage);
        UserAtom userAtom = userAtomRepository.findByAtomUri(atomUri);
        userAtom.setState(newState);
        // reload the user so we can save it
        // (the user object we get from getUserForSession is detached)
        userAtomRepository.save(userAtom);
    }

    private void deleteAtomUri(final WonMessage wonMessage, final WebSocketSession session) {
        User user = getUserForSession(session);
        URI atomUri = getOwnedAtomURI(wonMessage);
        // Get the atom from owner application db
        UserAtom userAtom = userAtomRepository.findByAtomUri(atomUri);
        if (userAtom.getState() == AtomState.DELETED) {
            // Get the user from owner application db
            user = userRepository.findOne(user.getId());
            // Delete atom in users atom list and save changes
            user.deleteAtomUri(userAtom);
            userRepository.save(user);
            // Delete atom in atom repository
            userAtomRepository.delete(userAtom.getId());
        } else {
            throw new IllegalStateException("atom not in state deleted");
        }
    }

    private User getUserForSession(final WebSocketSession session) {
        if (session == null) {
            return null;
        }
        if (session.getPrincipal() == null) {
            return null;
        }
        Principal principal = session.getPrincipal();
        if (principal instanceof Authentication) {
            return ((KeystoreEnabledUserDetails) ((Authentication) principal).getPrincipal()).getUser();
        }
        throw new IllegalStateException("no user found in session");
    }

    public void setOwnerApplicationService(final OwnerApplicationService ownerApplicationService) {
        this.ownerApplicationService = ownerApplicationService;
    }

    public void setEagerlyCachePopulatingProcessor(
                    EagerlyCachePopulatingMessageProcessor eagerlyCachePopulatingProcessor) {
        this.eagerlyCachePopulatingProcessor = eagerlyCachePopulatingProcessor;
    }

    public void setServerSideActionService(ServerSideActionService serverSideActionService) {
        this.serverSideActionService = serverSideActionService;
    }

    private URI getOwnedAtomURI(WonMessage message) {
        return message.getEnvelopeType() == WonMessageDirection.FROM_SYSTEM ? message.getSenderAtomURI()
                        : message.getRecipientAtomURI();
    }
}
