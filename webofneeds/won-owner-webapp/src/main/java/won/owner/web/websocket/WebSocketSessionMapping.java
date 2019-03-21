package won.owner.web.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * This service stores the connection between the WebSocket sessions and a given key
 *
 * @author Fabian Salcher
 */
public class WebSocketSessionMapping<T> {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  // ToDo (FS): make this persistent
  private Map<T, Set<WebSocketSession>> mapping = new HashMap<T, Set<WebSocketSession>>();
  private Object lock;

  public WebSocketSessionMapping() {
    this.lock = new Object();
  }

  public void addMapping(T key, WebSocketSession session) {
    logger.debug("adding mapping for key {} to websocket session {}", key, session.getId());
    synchronized (lock) {
      //we want to avoid losing one of two concurrent sessions added
      //for the same key, so we synchronize here
      if (!mapping.containsKey(key)) {
        //we use the CopyOnWriteArraySet so we are safe across threads. We
        //assume that reads outnumber writes by far.
        mapping.put(key, new CopyOnWriteArraySet<WebSocketSession>());
      }
    }
    mapping.get(key).add(session);
  }

  public void removeMapping(T key, WebSocketSession session) {
    logger.debug("removing mapping from key {} to websocket session {}", key, session.getId());
    synchronized (this) {
      //we don't want add and remove to interfere
      Set<WebSocketSession> sessions = mapping.get(key);
      if (sessions != null) {
        sessions.remove(session);
        if (sessions.isEmpty())
          mapping.remove(sessions);
      }
    }
  }

  public Set<WebSocketSession> getWebSocketSessions(T key) {
    Set<WebSocketSession> sessions = mapping.get(key);
    if (sessions != null) {
      Set<WebSocketSession> ret = new HashSet(sessions.size());
      ret.addAll(sessions);
      return ret;
    } else {
      return new HashSet();
    }
  }

}
