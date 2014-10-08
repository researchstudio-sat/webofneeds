package won.owner.web.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * This service stores the connection between the WebSocket sessions and the
 * Need URIs. If a OA client authenticates to a OA server it sends a list of
 * Need URIs which will then be mapped to the session to the client. If a
 * messageEvent comes through the session which has a unmapped Need URI the
 * URI will be mapped to the session as well.
 *
 * @author Fabian Salcher
 */
public class WebSocketSessionService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  // ToDo (FS): make this persistent
  private Map<URI, Set<WebSocketSession>> mapping = new HashMap<URI, Set<WebSocketSession>>();
  private Object lock ;

  public WebSocketSessionService() {
    this.lock = new Object();
  }


  public void addMapping(URI needURI, WebSocketSession session) {
    synchronized (lock) {
      //we want to avoid losing one of two concurrent sessions added
      //for the same needURI, so we synchronize here
      if (!mapping.containsKey(needURI)) {
        //we use the CopyOnWriteArraySet so we are safe across threads. We
        //assume that reads outnumber writes by far.
        mapping.put(needURI, new CopyOnWriteArraySet<WebSocketSession>());
      }
    }
    mapping.get(needURI).add(session);
  }

  public void removeMapping(URI needURI, WebSocketSession session) {
    synchronized (this) {
      //we don't want add and remove to interfere
      Set<WebSocketSession> sessions = mapping.get(needURI);
      if (sessions != null) {
        sessions.remove(session);
        if (sessions.isEmpty()) mapping.remove(sessions);
      }
    }
  }

  public Set<WebSocketSession> getWebSocketSessions(URI needURI) {
    Set<WebSocketSession> sessions = mapping.get(needURI);
    if (sessions != null) {
      Set<WebSocketSession> ret = new HashSet(sessions.size());
      ret.addAll(sessions);
      return ret;
    } else {
      return null;
    }
  }

}
