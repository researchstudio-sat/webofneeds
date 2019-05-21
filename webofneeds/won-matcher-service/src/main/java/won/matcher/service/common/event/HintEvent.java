package won.matcher.service.common.event;

import java.io.Serializable;
import java.net.URI;

import org.apache.jena.rdf.model.Model;

/**
 * Event is used to generate hints inside the matcher-service User: hfriedrich
 * Date: 23.06.2015
 */
public abstract class HintEvent implements Serializable {
    private static final long serialVersionUID = 2780602425768268632L;
    protected final String recipientWonNodeUri;
    protected final String targetWonNodeUri;
    protected final String matcherUri;
    protected final double score;
    protected URI generatedEventUri;
    protected final Cause cause;

    public HintEvent(String recipientWonNodeUri, String targetWonNodeUri,
                    String matcherUri, double score, Cause cause) {
        this.recipientWonNodeUri = recipientWonNodeUri;
        this.targetWonNodeUri = targetWonNodeUri;
        this.matcherUri = matcherUri;
        this.score = score;
        this.cause = cause;
    }

    public String getRecipientWonNodeUri() {
        return recipientWonNodeUri;
    }

    public String getTargetWonNodeUri() {
        return targetWonNodeUri;
    }

    public String getMatcherUri() {
        return matcherUri;
    }

    public double getScore() {
        return score;
    }

    public Cause getCause() {
        return cause;
    }

    public Model deserializeExplanationModel() {
        throw new UnsupportedOperationException();
    }

    public URI getGeneratedEventUri() {
        return generatedEventUri;
    }

    public void setGeneratedEventUri(final URI generatedEventUri) {
        this.generatedEventUri = generatedEventUri;
    }

    /**
     * Return a String that can be used to check if this HintEvent object means the
     * same hint as another object by comparing the strings for equality.
     * 
     * @return
     */
    public abstract String getIdentifyingString();
}