/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.owner.web.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
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
import won.owner.model.UserNeed;
import won.owner.repository.UserNeedRepository;
import won.owner.repository.UserRepository;
import won.owner.service.impl.OwnerApplicationService;
import won.owner.web.WonOwnerMailSender;
import won.protocol.message.*;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.util.WonRdfUtils;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * User: syim
 * Date: 06.08.14
 */
public class WonWebSocketHandler
    extends TextWebSocketHandler
    implements WonMessageProcessor, InitializingBean
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  //if we're receiving a partial message, a StringBuilder will be in the session's attributes map under this key
  private static final String SESSION_ATTRIBUTE_PARTIAL_MESSAGE = "partialMessage";

  private OwnerApplicationService ownerApplicationService;

  @Autowired
  private WebSocketSessionService webSocketSessionService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserNeedRepository userNeedRepository;

  @Autowired
  SessionRepository sessionRepository;

  @Autowired
  private WonOwnerMailSender emailSender;

  @Override
  public void afterPropertiesSet() throws Exception {
    this.ownerApplicationService.setMessageProcessorDelegate(this);
  }

  @Override
  public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
    super.afterConnectionEstablished(session);
    //remember which user or (if not logged in) which needUri the session is bound to
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
      for (UserNeed userNeed : user.getUserNeeds()){
        logger.debug("removing session bindings to need {}", userNeed.getUri());
        this.webSocketSessionService.removeMapping(userNeed.getUri(), session);
      }
    } else {
      logger.debug("connection closed, but no user found in session, no bindings removed");
    }
  }

  /*User user = getCurrentUser();

      logger.info("New Need:" + needPojo.getTextDescription() + "/" + needPojo.getCreationDate() + "/" +
        needPojo.getLongitude() + "/" + needPojo.getLatitude() + "/" + (needPojo.getState() == NeedState.ACTIVE));
      //TODO: using fixed Facets - change this
      needPojo.setFacetTypes(new String[]{
      FacetType.OwnerFacet.getURI().toString()});
      NeedPojo createdNeedPojo = resolve(needPojo);
      Need need = needRepository.findOne(createdNeedPojo.getNeedId());
      user.getNeeds().add(need);
      wonUserDetailService.save(user);
      HttpHeaders headers = new HttpHeaders();
      headers.setLocation(need.getNeedURI());
      return new ResponseEntity<NeedPojo>(createdNeedPojo, headers, HttpStatus.CREATED);     */
  @Override
  @Transactional(propagation = Propagation.SUPPORTS, isolation = Isolation.READ_COMMITTED)
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException
  {
    logger.debug("OA Server - WebSocket message received: {}", message.getPayload());
    updateSession(session);

    if (!message.isLast()){
      //we have an intermediate part of the current message.
      session.getAttributes()
             .putIfAbsent(SESSION_ATTRIBUTE_PARTIAL_MESSAGE, new StringBuilder());
    }
    //now check if we have the partial message string builder in the session.
    //if we do, we're processing a partial message, and we have to append the current message payload
    StringBuilder sb = (StringBuilder) session.getAttributes().get(SESSION_ATTRIBUTE_PARTIAL_MESSAGE);
    String completePayload = null; //will hold the final message
    if (sb == null) {
      //No string builder found in the session - we're not processing a partial message.
      //The complete payload is in the current message. Get it and continue.
      completePayload = message.getPayload();
    } else {
      //the string builder is there - we're processing a partial message. append the current piece
      sb.append(message.getPayload());
      if (message.isLast()){
        //we've received the last part. pass it on to the next processing steps.
        completePayload = sb.toString();
        //also, we do not need the string builder in the session any longer. remove it:
        session.getAttributes().remove(SESSION_ATTRIBUTE_PARTIAL_MESSAGE);
      } else {
        //This is not the last part of the message.
        //We have stored it along with all previous parts. Abort processing this message and wait for the
        // next part
        return;
      }
    }

    WonMessage wonMessage = WonMessageDecoder.decodeFromJsonLd(completePayload);
    //remember which user or (if not logged in) which needUri the session is bound to
    User user = getUserForSession(session);
    if (user != null) {
      logger.debug("binding session to user {}", user.getId());
      this.webSocketSessionService.addMapping(user, session);
    }
    //anyway, we have to bind the URI to the session, otherwise we can't handle incoming server->client messages
    URI needUri = wonMessage.getSenderNeedURI();
    logger.debug("binding session to need URI {}", needUri);
    this.webSocketSessionService.addMapping(needUri, session);

    ownerApplicationService.sendWonMessage(wonMessage);
  }

  /* update the session last accessed time, - spring-session was added to synchronize
  // http sessions with websocket session
  // see: http://spring.io/blog/2014/09/16/preview-spring-security-websocket-support-sessions
  // Currently used here Sping's MapSession implementation of Spring has default timeout of 30 minutes
  */
  private void updateSession(final WebSocketSession session) {
    String sessionId = (String) session.getAttributes().get(WonHandshakeInterceptor.SESSION_ATTR);
    if (sessionId != null) {
      Session activeSession = sessionRepository.getSession(sessionId);
      if (session != null) {
        sessionRepository.save(activeSession);
      }
    }
  }

  /**
   * We want to keep the buffer in the underlying server small (8k per websocket), but still
   * be able to receive large messages. Hence, we have to be able to handle partial messages here.
   */
  @Override
  public boolean supportsPartialMessages() {
    return true;
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS, isolation = Isolation.READ_COMMITTED)
  public WonMessage process(final WonMessage wonMessage) {

    String wonMessageJsonLdString = WonMessageEncoder.encodeAsJsonLd(wonMessage);
    WebSocketMessage<String> webSocketMessage = new TextMessage(wonMessageJsonLdString);
    URI needUri = wonMessage.getReceiverNeedURI();
    User user = getUserForWonMessage(wonMessage);

    Set<WebSocketSession> webSocketSessions = findWebSocketSessionsForWonMessage(wonMessage, needUri, user);

    //check if we can deliver the message. If not, send email.
    if (webSocketSessions.size() == 0) {
      logger.info("cannot deliver message of type {} for need {}, receiver {}: no websocket session found",
        new Object[]{wonMessage.getMessageType(),
                     wonMessage.getReceiverNeedURI(),
                     wonMessage.getReceiverURI()});
      // send per email notifications if it applies:
      notifyPerEmail(user, needUri, wonMessage);
      return wonMessage;
    }
    for (WebSocketSession session : webSocketSessions) {
      sendMessageForSession(wonMessage, webSocketMessage, session, needUri, user);
    }
    return wonMessage;
  }

  private void notifyPerEmail(final User user, final URI needUri, final WonMessage wonMessage) {

    if (user == null) {
      return;
    }

    UserNeed userNeed = getNeedOfUser(user, needUri);
    if (userNeed == null) {
      return;
    }

    String textMsg = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);

    try {
      switch (wonMessage.getMessageType()) {
        case OPEN:
          if (userNeed.isConversations()) {
            emailSender.sendConversationNotificationHtmlMessage(
              user.getEmail(), needUri.toString(), wonMessage.getSenderNeedURI().toString(), wonMessage.getReceiverURI().toString(), textMsg);
          }
          return;
        case CONNECTION_MESSAGE:
          if (userNeed.isConversations()) {
            emailSender.sendConversationNotificationHtmlMessage(
              user.getEmail(), needUri.toString(), wonMessage.getSenderNeedURI().toString(), wonMessage.getReceiverURI().toString(), textMsg);
          }
          return;
        case CONNECT:
          if (userNeed.isRequests()) {
            emailSender.sendConnectNotificationHtmlMessage(
              user.getEmail(), needUri.toString(), wonMessage.getSenderNeedURI().toString(), wonMessage.getReceiverURI().toString(), textMsg);
          }
          return;
        case HINT_MESSAGE:
          if (userNeed.isMatches()) {
            String remoteNeedUri = WonRdfUtils.MessageUtils.toMatch(wonMessage).getToNeed().toString();
            emailSender.sendHintNotificationMessageHtml(user.getEmail(), needUri.toString(), remoteNeedUri,wonMessage
              .getReceiverURI().toString());
          }
          return;
        case CLOSE:
          // do not send emails for a close
          return;
        case DEACTIVATE:
          // a deactivate message, coming from the WoN node. Always deliverd by email.
          emailSender.sendSystemDeactivateNotificationHtmlMessage(
                    user.getEmail(), needUri.toString(), textMsg);

          return;
        case NEED_MESSAGE:
          // a need message, coming from the WoN node. Always deliverd by email.
            emailSender.sendSystemDeactivateNotificationHtmlMessage(
                    user.getEmail(), needUri.toString(), textMsg);
          return;
        default:
          return;
      }
    } catch (MailException ex) { // org.springframework.mail.MailException
      logger.error("Email could not be sent", ex);
    }
  }

  private Set<WebSocketSession> findWebSocketSessionsForWonMessage(final WonMessage wonMessage, URI needUri,
    User user) {
    assert wonMessage != null : "wonMessage must not be null";
    assert needUri != null : "needUri must not be null";
    Set<WebSocketSession> webSocketSessions =
        webSocketSessionService.getWebSocketSessions(needUri);
    if (webSocketSessions == null) webSocketSessions = new HashSet();
    logger.debug("found {} sessions for need uri {}, now removing closed sessions", webSocketSessions.size(), needUri);
    removeClosedSessions(webSocketSessions, needUri);
    if (user != null){
      Set<WebSocketSession> userSessions = webSocketSessionService.getWebSocketSessions(user);
      if (userSessions == null) userSessions = new HashSet();
      logger.debug("found {} sessions for user {}, now removing closed sessions", userSessions.size(), user.getId());
      removeClosedSessions(userSessions, user);
      webSocketSessions.addAll(userSessions);
    }
    return webSocketSessions;
  }

  private void removeClosedSessions(final Set<WebSocketSession> webSocketSessions, final URI needUri) {
    for (Iterator<WebSocketSession> it = webSocketSessions.iterator(); it.hasNext(); ){
      WebSocketSession session = it.next();
      if (!session.isOpen()) {
        logger.debug("removing closed websocket session {} of need {}", session.getId(), needUri);
        webSocketSessionService.removeMapping(needUri, session);
        it.remove();
      }
    }
  }

  private void removeClosedSessions(final Set<WebSocketSession> webSocketSessions, final User user) {
    for (Iterator<WebSocketSession> it = webSocketSessions.iterator(); it.hasNext(); ){
      WebSocketSession session = it.next();
      if (!session.isOpen()) {
        logger.debug("removing closed websocket session {} of user {}", session.getId(), user.getId());
        webSocketSessionService.removeMapping(user, session);
        it.remove();
      }
    }
  }


  private User getUserForWonMessage(final WonMessage wonMessage) {
    URI needUri = wonMessage.getReceiverNeedURI();
    return userRepository.findByNeedUri(needUri);
  }

  private UserNeed getNeedOfUser(final User user, final URI needUri) {

    for (UserNeed userNeed : user.getUserNeeds()) {
      if (userNeed.getUri().equals(needUri)) {
        return userNeed;
      }
    }
    return null;
  }

  private synchronized void sendMessageForSession(final WonMessage wonMessage, final WebSocketMessage<String>
    webSocketMessage,
    final WebSocketSession session, URI needUri, User user) {
    if (!session.isOpen()){
      logger.debug("session {} is closed, can't send message", session.getId());
      return;
    }
    if (wonMessage.getMessageType() == WonMessageType.SUCCESS_RESPONSE
      && WonMessageType.CREATE_NEED ==wonMessage.getIsResponseToMessageType()){

      if (session.getPrincipal() != null) {
        saveNeedUriWithUser(wonMessage, session);
      } else {
        logger.warn("could not associate need {} with currently logged in user: no principal found in session");
      }
    }
    try {
      logger.debug("OA Server - sending WebSocket message: {}", webSocketMessage);
      session.sendMessage(webSocketMessage);
    } catch (Exception e) {
      logger.warn(MessageFormat.format("caught exception while trying to send on session {1} for needUri {2}, " +
          "user {3}", session.getId(), needUri, user), e);
        if (user != null){
          webSocketSessionService.removeMapping(user, session);
        }
        if (needUri != null) {
          webSocketSessionService.removeMapping(needUri, session);
        }
    }
  }

  private void saveNeedUriWithUser(final WonMessage wonMessage, final WebSocketSession session) {
    User user = getUserForSession(session);
    URI needURI = wonMessage.getReceiverNeedURI();
    UserNeed userNeed = new UserNeed(needURI);
    userNeedRepository.save(userNeed);
    user.addNeedUri(userNeed);
    userRepository.save(user);
  }

  private User getUserForSession(final WebSocketSession session) {
    if (session == null) {
      return null;
    }
    if (session.getPrincipal() == null){
      return null;
    }
    String username = session.getPrincipal().getName();
    return userRepository.findByUsername(username);
  }


  public void setOwnerApplicationService(final OwnerApplicationService ownerApplicationService) {
    this.ownerApplicationService = ownerApplicationService;
  }

}
