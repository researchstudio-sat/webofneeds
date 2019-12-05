package won.bot.framework.eventbot.event.impl.wonmessage;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.MessageEvent;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;

public abstract class HintFromMatcherEvent extends BaseEvent implements MessageEvent {
    private final WonMessage wonMessage;
    private double hintScore;
    private URI targetAtomURI;
    private URI recipientAtomURI;

    HintFromMatcherEvent(WonMessage wonMessage) {
        this.wonMessage = wonMessage;
        this.hintScore = wonMessage.getHintScore();
        this.targetAtomURI = WonMessageUtils.getHintTargetAtomURIRequired(wonMessage);
        this.recipientAtomURI = WonMessageUtils.getRecipientAtomURIRequired(wonMessage);
    }

    public final double getHintScore() {
        return hintScore;
    }

    public URI getHintTargetAtom() {
        return targetAtomURI;
    }

    public URI getRecipientAtom() {
        return recipientAtomURI;
    }

    @Override
    public final WonMessage getWonMessage() {
        return wonMessage;
    }

    @Override
    public final WonMessageType getWonMessageType() {
        return this.wonMessage.getMessageType();
    }
}
