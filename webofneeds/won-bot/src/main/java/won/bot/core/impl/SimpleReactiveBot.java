package won.bot.core.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.core.base.BasicServiceBot;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.util.MessageModelUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple bot that connects with all matched needs, opens all connections, sends a number of text message and closes.
 */
public class SimpleReactiveBot extends BasicServiceBot {
  private Map<URI, Integer> messageCountPerConnection = new HashMap<URI, Integer>();
  private static final int MAX_MESSAGE_COUNT = 3;

  @Override
  public void onConnectFromOtherNeed(Connection con) throws Exception {
    getOwnerService().open(con.getConnectionURI(), null);
  }

  @Override
  public void onHintFromMatcher(Match match) throws Exception {
    getOwnerService().connect(match.getFromNeed(), match.getToNeed(), null);
  }

  @Override
  public void onMessageFromOtherNeed(Connection con, ChatMessage message) throws Exception {
    sendNextMessageViaConnectionOrClose(con);
  }

  @Override
  public void onNewNeedCreated(URI needUri, URI wonNodeUri, Model needModel) {
    //do nothing
  }

  @Override
  public void onOpenFromOtherNeed(Connection con) throws Exception {
    sendNextMessageViaConnectionOrClose(con);
  }

  private void sendNextMessageViaConnectionOrClose(Connection con) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    synchronized (this) {
      int msgCount = this.messageCountPerConnection.get(con.getConnectionURI());
      if (msgCount < MAX_MESSAGE_COUNT){
        msgCount++;
        Model messageModel = MessageModelUtils.textMessage("message " + msgCount + " [" + con.getConnectionURI().toString() + "]");
        getOwnerService().textMessage(con.getConnectionURI(), messageModel);
        this.messageCountPerConnection.put(con.getConnectionURI(), msgCount);
      } else {
        getOwnerService().close(con.getConnectionURI(), null);
      }
    }
  }
}
