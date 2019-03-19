package won.protocol.agreement.effect;

import java.net.URI;

public class Claims extends MessageEffect {

    private URI claimedMessageUri;

    public Claims(URI messageUri, URI claimedMessageUri) {
        super(messageUri, MessageEffectType.CLAIMS);
        this.claimedMessageUri = claimedMessageUri;
    }

    public URI getClaimedMessageUri() {
        return claimedMessageUri;
    }

    @Override
    public String toString() {
        return "Claims [claimedMessageUri=" + claimedMessageUri + "]";
    }

}
