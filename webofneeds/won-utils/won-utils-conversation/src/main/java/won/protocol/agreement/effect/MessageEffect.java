package won.protocol.agreement.effect;

import java.net.URI;

public abstract class MessageEffect {

    private final URI messageUri;
    private final MessageEffectType type;

    public MessageEffect(URI messageUri, MessageEffectType types) {
        super();
        this.messageUri = messageUri;
        this.type = types;
    }

    public URI getMessageUri() {
        return messageUri;
    }

    public MessageEffectType getType() {
        return type;
    }

    public boolean isAccepts() {
        return type == MessageEffectType.ACCEPTS;
    }

    public boolean isProposes() {
        return type == MessageEffectType.PROPOSES;
    }

    public boolean isRejects() {
        return type == MessageEffectType.REJECTS;
    }

    public boolean isRetracts() {
        return type == MessageEffectType.RETRACTS;
    }

    public boolean isClaims() {
        return type == MessageEffectType.CLAIMS;
    }

    public Accepts asAccepts() {
        return (Accepts) this;
    }

    public Proposes asProposes() {
        return (Proposes) this;
    }

    public Rejects asRejects() {
        return (Rejects) this;
    }

    public Retracts asRetracts() {
        return (Retracts) this;
    }

    public Claims asClaims() {
        return (Claims) this;
    }
}
