package won.owner.web.websocket;

import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

  // ToDo (FS): make this persistent
  private Map<URI, Set<WebSocketSession>> mapping = new HashMap<URI, Set<WebSocketSession>>();

  public void addMapping(URI needURI, WebSocketSession session) {
    if (!mapping.containsKey(needURI)) {
      mapping.put(needURI, new HashSet<WebSocketSession>());
    }
    mapping.get(needURI).add(session);
  }

  public void removeMapping(URI needURI, WebSocketSession session) {
    if (mapping.containsKey(needURI))
      mapping.get(needURI).remove(session);
  }

  public Set<WebSocketSession> getWebSocketSessions(URI needURI) {
    if (mapping.containsKey(needURI))
      return mapping.get(needURI);
    else
      return null;
  }
}
