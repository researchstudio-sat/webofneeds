package won.bot.framework.events.listener.baStateBots;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 5.3.14.
 * Time: 12.39
 * To change this template use File | Settings | File Templates.
 */
public class BATestScriptAction {
    private boolean senderIsParticipant;
    private String messageToBeSent;
    private URI stateOfSenderBeforeSending;

    public BATestScriptAction(boolean senderIsParticipant, String messageToBeSent, URI stateOfSenderBeforeSending) {
        this.senderIsParticipant = senderIsParticipant;
        this.messageToBeSent = messageToBeSent;
        this.stateOfSenderBeforeSending = stateOfSenderBeforeSending;
    }

    public boolean isSenderIsParticipant() {
        return senderIsParticipant;
    }

    public boolean isSenderIsCoordinator(){
        return ! isSenderIsParticipant();
    }

    public String getMessageToBeSent() {
        return messageToBeSent;
    }

    public URI getStateOfSenderBeforeSending() {
        return stateOfSenderBeforeSending;
    }
}
