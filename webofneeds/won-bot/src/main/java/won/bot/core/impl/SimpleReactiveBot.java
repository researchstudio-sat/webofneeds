package won.bot.core.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.core.base.BasicServiceBot;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Match;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple bot that connects with all matched needs, opens all connections, sends a number of text message and closes.
 */
public class SimpleReactiveBot extends BasicServiceBot {
  private Map<URI, Integer> messageCountPerConnection = new HashMap<URI, Integer>();
  private int messageCount = 3;

  @Override
  public void onConnectFromOtherNeed(Connection con, final Model content) throws Exception {
    logger.debug("bot received connect for need {}, connection {}", con.getNeedURI(), con.getConnectionURI());
    getOwnerService().open(con.getConnectionURI(), null);
  }

  @Override
  public void onHintFromMatcher(Match match, final Model content) throws Exception {
    logger.debug("bot received hint for need {}", match.getFromNeed());
    getOwnerService().connect(match.getFromNeed(), match.getToNeed(), null);
  }

  @Override
  public void onMessageFromOtherNeed(Connection con, ChatMessage message, final Model content) throws Exception {
    logger.debug("bot received message for need {}, connection {}", con.getNeedURI(), con.getConnectionURI());
    sendNextMessageViaConnectionOrClose(con);
  }

  @Override
  public void onNewNeedCreated(URI needUri, URI wonNodeUri, Model needModel) {
    logger.debug("bot created new need {} on won node {}", needUri, wonNodeUri);
    //do nothing
  }

  @Override
  public void onOpenFromOtherNeed(Connection con, final Model content) throws Exception {
    logger.debug("bot received open for need {}, connection {}", con.getNeedURI(), con.getConnectionURI());
    sendNextMessageViaConnectionOrClose(con);
  }

  @Override
  public void onCloseFromOtherNeed(final Connection con, final Model content) throws Exception
  {
    logger.debug("bot received close for need {}, connection {}", con.getNeedURI(), con.getConnectionURI());
    //do nothing
  }

  /**
   * Set the number of messages to send before closing.
   * @param messageCount
   */
  public void setMessageCount(final int messageCount)
  {
    this.messageCount = messageCount;
  }

  private void sendNextMessageViaConnectionOrClose(Connection con) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    if (con.getState() == ConnectionState.CLOSED) return;
    synchronized (this) {
      int msgCount = this.messageCountPerConnection.get(con.getConnectionURI());
      if (msgCount < messageCount){
        msgCount++;
        Model messageModel = WonRdfUtils.textMessage("message " + msgCount + " [" + con.getConnectionURI().toString() + "]");
        getOwnerService().textMessage(con.getConnectionURI(), messageModel);
        this.messageCountPerConnection.put(con.getConnectionURI(), msgCount);
      } else {
        getOwnerService().close(con.getConnectionURI(), null);
      }
    }
  }
}
