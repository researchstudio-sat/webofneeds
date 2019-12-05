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

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import won.owner.model.User;

public class WebSocketSessionService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private WebSocketSessionMapping<Long> userIdToSession = new WebSocketSessionMapping<Long>();
    private WebSocketSessionMapping<URI> atomUriToSession = new WebSocketSessionMapping<URI>();

    public void addMapping(User user, WebSocketSession session) {
        this.userIdToSession.addMapping(user.getId(), session);
    }

    public void addMapping(URI atomUri, WebSocketSession session) {
        this.atomUriToSession.addMapping(atomUri, session);
    }

    public void removeMapping(User user, WebSocketSession session) {
        this.userIdToSession.removeMapping(user.getId(), session);
    }

    public void removeMapping(URI atomUri, WebSocketSession session) {
        this.atomUriToSession.removeMapping(atomUri, session);
    }

    public Set<WebSocketSession> getWebSocketSessions(User user) {
        return this.userIdToSession.getWebSocketSessions(user.getId());
    }

    public Set<WebSocketSession> getWebSocketSessions(URI atomUri) {
        return this.atomUriToSession.getWebSocketSessions(atomUri);
    }

    public Set<WebSocketSession> findWebSocketSessionsForAtomAndUser(URI atomUri, User user) {
        Set<WebSocketSession> webSocketSessions = getWebSocketSessions(atomUri);
        if (webSocketSessions == null)
            webSocketSessions = new HashSet();
        logger.debug("found {} sessions for atom uri {}, now removing closed sessions", webSocketSessions.size(),
                        atomUri);
        removeClosedSessions(webSocketSessions, atomUri);
        if (user != null) {
            Set<WebSocketSession> userSessions = getWebSocketSessions(user);
            if (userSessions == null)
                userSessions = new HashSet();
            logger.debug("found {} sessions for user {}, now removing closed sessions", userSessions.size(),
                            user.getId());
            removeClosedSessions(userSessions, user);
            webSocketSessions.addAll(userSessions);
        }
        return webSocketSessions;
    }

    public void removeClosedSessions(final Set<WebSocketSession> webSocketSessions, final URI atomUri) {
        for (Iterator<WebSocketSession> it = webSocketSessions.iterator(); it.hasNext();) {
            WebSocketSession session = it.next();
            if (!session.isOpen()) {
                logger.debug("removing closed websocket session {} of atom {}", session.getId(), atomUri);
                removeMapping(atomUri, session);
                it.remove();
            }
        }
    }

    public void removeClosedSessions(final Set<WebSocketSession> webSocketSessions, final User user) {
        for (Iterator<WebSocketSession> it = webSocketSessions.iterator(); it.hasNext();) {
            WebSocketSession session = it.next();
            if (!session.isOpen()) {
                logger.debug("removing closed websocket session {} of user {}", session.getId(), user.getId());
                removeMapping(user, session);
                it.remove();
            }
        }
    }
}
