package won.matcher.service.common.event;

public class SocketHintEvent extends HintEvent {
    private static final long serialVersionUID = -5154547684180077065L;
    private String recipientSocketUri;
    private String targetSocketUri;

    public SocketHintEvent(
                    String recipientSocketUri,
                    String recipientWonNodeUri,
                    String targetSocketUri,
                    String targetWonNodeUri,
                    String matcherUri,
                    double score,
                    Cause cause) {
        super(recipientWonNodeUri, targetWonNodeUri, matcherUri, score, cause);
        this.recipientSocketUri = recipientSocketUri;
        this.targetSocketUri = targetSocketUri;
    }

    public String getRecipientSocketUri() {
        return recipientSocketUri;
    }

    public String getTargetSocketUri() {
        return targetSocketUri;
    }

    public SocketHintEvent clone() {
        SocketHintEvent clone = new SocketHintEvent(recipientSocketUri, recipientSocketUri, targetSocketUri,
                        targetSocketUri, recipientSocketUri, serialVersionUID, cause);
        if (generatedEventUri != null) {
            clone.setGeneratedEventUri(generatedEventUri);
        }
        return clone;
    }

    @Override
    public String getIdentifyingString() {
        return "SocketHintEvent" + targetSocketUri + recipientSocketUri;
    }

    @Override
    public String toString() {
        return "SocketHintEvent [recipientSocketUri=" + recipientSocketUri + ", targetSocketUri=" + targetSocketUri
                        + ", recipientWonNodeUri=" + recipientWonNodeUri + ", targetWonNodeUri=" + targetWonNodeUri
                        + ", matcherUri=" + matcherUri + ", score=" + score + ", generatedEventUri=" + generatedEventUri
                        + ", cause=" + cause + "]";
    }
}
