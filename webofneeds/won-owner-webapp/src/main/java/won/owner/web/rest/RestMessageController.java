package won.owner.web.rest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import won.owner.model.User;
import won.owner.pojo.MessageUriPojo;
import won.owner.pojo.RestStatusResponse;
import won.owner.repository.UserRepository;
import won.owner.service.impl.KeystoreEnabledUserDetails;
import won.owner.service.impl.OwnerApplicationService;
import won.owner.web.service.UserAtomService;
import won.owner.web.websocket.WebSocketSessionService;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.sender.exception.WonMessageSenderException;
import won.protocol.util.AuthenticationThreadLocal;

@Controller
@RequestMapping("/rest/messages")
public class RestMessageController {
    Logger logger = LoggerFactory.getLogger(getClass());

    public RestMessageController() {
    }

    @Autowired
    OwnerApplicationService ownerApplicationService;
    @Autowired
    UserAtomService userAtomService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WebSocketSessionService webSocketSessionService;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Prepares and sends the WonMessage received in the body of this post request.
     * The message has to be encoded in JSON-LD and use the reserved self message
     * URI.
     * 
     * @param message
     * @return
     */
    @RequestMapping(value = "/send", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, consumes = "application/ld+json")
    public ResponseEntity send(@RequestBody String serializedWonMessage) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null) {
            return generateStatusResponse(RestStatusResponse.USER_NOT_SIGNED_IN, Optional.empty());
        }
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        WonMessage msg = null;
        // decode the message
        try {
            msg = WonMessageDecoder.decode(Lang.JSONLD, serializedWonMessage);
        } catch (Exception e) {
            logger.debug("Error parsing message, returning error response", e);
            return generateStatusResponse(RestStatusResponse.ERROR_PARSING_MESSAGE, Optional.of(getErrorMessage(e)));
        }
        // prepare it
        try {
            AuthenticationThreadLocal.setAuthentication(authentication);
            msg = ownerApplicationService.prepareMessage(msg);
        } catch (Exception e) {
            logger.debug("Error preparing message, returning error response", e);
            return generateStatusResponse(RestStatusResponse.ERROR_PREPARING_MESSAGE, Optional.of(getErrorMessage(e)));
        } finally {
            // be sure to remove the principal from the threadlocal
            AuthenticationThreadLocal.remove();
        }
        // associate all the user's websocket sessions with
        // the sender atom so that we can route the response properly
        User user = getUser(authentication, msg);
        URI atomURI = msg.getSenderAtomURI();
        if (user != null && atomURI != null) {
            webSocketSessionService.getWebSocketSessions(user).forEach(
                            session -> webSocketSessionService.addMapping(atomURI, session));
        }
        // send it in a separate thread (so we can return our result immediately)
        try {
            final WonMessage preparedMessage = msg;
            executor.submit(() -> {
                ownerApplicationService.sendMessage(preparedMessage);
            });
        } catch (Exception e) {
            logger.debug("Error sending message, returning error response", e);
            return generateStatusResponse(RestStatusResponse.ERROR_SENDING_MESSAGE_TO_NODE,
                            Optional.of(getErrorMessage(e)));
        }
        return new ResponseEntity(new MessageUriPojo(msg.getMessageURIRequired().toString()), HttpStatus.OK);
    }

    private User getUser(Authentication auth, WonMessage message) {
        User user = ((KeystoreEnabledUserDetails) auth.getPrincipal()).getUser();
        if (user != null) {
            return user;
        }
        return getUserForWonMessage(message);
    }

    private User getUserForWonMessage(final WonMessage wonMessage) {
        URI atomUri = getOwnedAtomURI(wonMessage);
        return userRepository.findByAtomUri(atomUri);
    }

    private URI getOwnedAtomURI(WonMessage message) {
        return message.getEnvelopeType() == WonMessageDirection.FROM_SYSTEM ? message.getSenderAtomURI()
                        : message.getRecipientAtomURI();
    }

    private String getErrorMessage(Throwable e) {
        if (e instanceof WonMessageSenderException || e instanceof WonMessageProcessingException) {
            if (e.getCause() != null) {
                return e.getMessage() + " - Caused by: " + getErrorMessage(e.getCause());
            }
        }
        return e.getMessage();
    }

    private static ResponseEntity<Map<String, Object>> generateStatusResponse(RestStatusResponse restStatusResponse,
                    Optional<String> detail) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("code", restStatusResponse.getCode());
        values.put("message", restStatusResponse.getMessage());
        detail.ifPresent(d -> values.put("detail", d));
        return new ResponseEntity<Map<String, Object>>(values, restStatusResponse.getHttpStatus());
    }
}
