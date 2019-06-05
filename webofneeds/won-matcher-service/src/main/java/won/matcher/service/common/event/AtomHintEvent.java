package won.matcher.service.common.event;

public class AtomHintEvent extends HintEvent {
    private static final long serialVersionUID = -1014000498153051628L;
    private final String targetAtomUri;
    private final String recipientAtomUri;

    public AtomHintEvent(
                    String recipientAtomUri,
                    String recipientWonNodeUri,
                    String targetAtomUri,
                    String targetWonNodeUri,
                    String matcherUri,
                    double score,
                    Cause cause) {
        super(recipientWonNodeUri, targetWonNodeUri, matcherUri, score, cause);
        this.targetAtomUri = targetAtomUri;
        this.recipientAtomUri = recipientAtomUri;
    }

    public String getTargetAtomUri() {
        return targetAtomUri;
    }

    public String getRecipientAtomUri() {
        return recipientAtomUri;
    }

    public AtomHintEvent clone() {
        AtomHintEvent clone = new AtomHintEvent(recipientAtomUri, recipientWonNodeUri, targetAtomUri, targetWonNodeUri,
                        matcherUri, score, cause);
        if (generatedEventUri != null)
            clone.setGeneratedEventUri(generatedEventUri);
        return clone;
    }

    @Override
    public String getIdentifyingString() {
        return "AtomHintEvent" + targetAtomUri + recipientAtomUri;
    }

    @Override
    public String toString() {
        return "AtomHintEvent [targetAtomUri=" + targetAtomUri + ", recipientAtomUri=" + recipientAtomUri
                        + ", recipientWonNodeUri=" + recipientWonNodeUri + ", targetWonNodeUri=" + targetWonNodeUri
                        + ", matcherUri=" + matcherUri + ", score=" + score + ", generatedEventUri=" + generatedEventUri
                        + ", cause=" + cause + "]";
    }
}
