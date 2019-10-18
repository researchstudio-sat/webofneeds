package won.bot.framework.eventbot.event.impl.wonmessage;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.MessageEvent;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;

import java.net.URI;
import java.util.Optional;

public abstract class HintFromMatcherEvent extends BaseEvent implements MessageEvent {
    private final WonMessage wonMessage;
    private double hintScore;
    private URI targetAtomURI;
    private URI recipientAtomURI;

    HintFromMatcherEvent(WonMessage wonMessage) {
        this.wonMessage = wonMessage;
        this.hintScore = wonMessage.getHintScore();
        this.targetAtomURI = wonMessage.getHintTargetAtomURI();
        this.recipientAtomURI = wonMessage.getRecipientAtomURI();
    }

    public final double getHintScore() {
        return hintScore;
    }

    public Optional<URI> getTargetAtomURI() {
        return Optional.of(targetAtomURI);
    }

    public Optional<URI> getRecipientAtomURI() {
        return Optional.of(recipientAtomURI);
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
