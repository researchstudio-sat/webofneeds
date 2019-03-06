package won.bot.framework.eventbot.listener.baStateBots;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.ResourceImpl;

import won.node.facet.impl.WON_TX;
import won.protocol.util.WonRdfUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 5.3.14.
 * Time: 12.39
 * To change this template use File | Settings | File Templates.
 */
public class BATestScriptAction {
    private boolean senderIsParticipant;
    private Model messageToBeSent;
    private URI stateOfSenderBeforeSending;

    /**
     * Constructor that will cause the script to send text messages.
     * @param senderIsParticipant
     * @param messageToBeSent
     * @param stateOfSenderBeforeSending
     */
    public BATestScriptAction(boolean senderIsParticipant, String messageToBeSent, URI stateOfSenderBeforeSending) {
        this.senderIsParticipant = senderIsParticipant;
        this.messageToBeSent = WonRdfUtils.MessageUtils.textMessage(messageToBeSent);
        this.stateOfSenderBeforeSending = stateOfSenderBeforeSending;
    }

    /**
     * Constructor that will cause the script to send won:coordinationMessage with the given URI as object.
     * @param senderIsParticipant
     * @param coordinationMessageUriToBeSent
     * @param stateOfSenderBeforeSending
     */
    public BATestScriptAction(boolean senderIsParticipant, URI coordinationMessageUriToBeSent,URI stateOfSenderBeforeSending) {
        this.senderIsParticipant = senderIsParticipant;
        this.stateOfSenderBeforeSending = stateOfSenderBeforeSending;
        this.messageToBeSent = WonRdfUtils.MessageUtils.genericMessage(WON_TX.COORDINATION_MESSAGE, new ResourceImpl(coordinationMessageUriToBeSent.toString()));
    }

    public boolean isSenderIsParticipant() {
        return senderIsParticipant;
    }

    public boolean isSenderIsCoordinator(){
        return ! isSenderIsParticipant();
    }

    public Model getMessageToBeSent() {
        return messageToBeSent;
    }

    public URI getStateOfSenderBeforeSending() {
        return stateOfSenderBeforeSending;
    }

    public boolean isNopAction(){
      return false;
    }

  @Override
  public String toString() {
    return "BATestScriptAction{" +
      "senderIsParticipant=" + senderIsParticipant +
      ", messageToBeSent=" + messageToBeSent +
      ", stateOfSenderBeforeSending=" + stateOfSenderBeforeSending +
      '}';
  }
}
