package won.matcher.service.common.event;

import java.io.Serializable;
import java.net.URI;

import org.apache.jena.rdf.model.Model;

/**
 * Event is used to generate hints inside the matcher-service User: hfriedrich
 * Date: 23.06.2015
 */
public class HintEvent implements Serializable {
    private String fromAtomUri;
    private String fromWonNodeUri;
    private String toAtomUri;
    private String toWonNodeUri;
    private String matcherUri;
    private double score;
    private URI generatedEventUri;
    private String serializedExplanationModel;
    private String serializationLangName;
    private String serializationLangContentType;
    private Cause cause;

    public HintEvent(String fromWonNodeUri, String fromAtomUri, String toWonNodeUri, String toAtomUri,
                    String matcherUri, double score, Cause cause) {
        this.fromWonNodeUri = fromWonNodeUri;
        this.fromAtomUri = fromAtomUri;
        this.toWonNodeUri = toWonNodeUri;
        this.toAtomUri = toAtomUri;
        this.matcherUri = matcherUri;
        this.score = score;
        this.cause = cause;
    }

    public String getFromAtomUri() {
        return fromAtomUri;
    }

    public String getToAtomUri() {
        return toAtomUri;
    }

    public String getFromWonNodeUri() {
        return fromWonNodeUri;
    }

    public String getToWonNodeUri() {
        return toWonNodeUri;
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

    public void setSerializedExplanationModel(final String serializedExplanationModel) {
        this.serializedExplanationModel = serializedExplanationModel;
    }

    public void setSerializationLangName(final String serializationLangName) {
        this.serializationLangName = serializationLangName;
    }

    public void setSerializationLangContentType(final String serializationLangContentType) {
        this.serializationLangContentType = serializationLangContentType;
    }

    @Override
    public HintEvent clone() {
        HintEvent e = new HintEvent(fromWonNodeUri, fromAtomUri, toWonNodeUri, toAtomUri, matcherUri, score, cause);
        e.setGeneratedEventUri(this.getGeneratedEventUri());
        e.setSerializationLangContentType(this.serializationLangContentType);
        e.setSerializationLangName(this.serializationLangName);
        e.setSerializedExplanationModel(this.serializedExplanationModel);
        return e;
    }

    @Override
    public String toString() {
        return "HintEvent: (" + getFromWonNodeUri() + ", " + getFromAtomUri() + ", " + getToWonNodeUri() + ", "
                        + getToAtomUri() + ", " + getMatcherUri() + ", " + getScore() + ", " + getCause() + ")";
    }
}