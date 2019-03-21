package won.matcher.service.common.event;

import org.apache.jena.rdf.model.Model;

import java.io.Serializable;
import java.net.URI;

/**
 * Event is used to generate hints inside the matcher-service
 * <p>
 * User: hfriedrich Date: 23.06.2015
 */
public class HintEvent implements Serializable {
  private String fromNeedUri;
  private String fromWonNodeUri;
  private String toNeedUri;
  private String toWonNodeUri;

  private String matcherUri;
  private double score;

  private URI generatedEventUri;

  private String serializedExplanationModel;
  private String serializationLangName;
  private String serializationLangContentType;

  public HintEvent(String fromWonNodeUri, String fromNeedUri, String toWonNodeUri, String toNeedUri, String matcherUri,
      double score) {

    this.fromWonNodeUri = fromWonNodeUri;
    this.fromNeedUri = fromNeedUri;
    this.toWonNodeUri = toWonNodeUri;
    this.toNeedUri = toNeedUri;
    this.matcherUri = matcherUri;
    this.score = score;
  }

  public String getFromNeedUri() {
    return fromNeedUri;
  }

  public String getToNeedUri() {
    return toNeedUri;
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
    HintEvent e = new HintEvent(fromWonNodeUri, fromNeedUri, toWonNodeUri, toNeedUri, matcherUri, score);
    e.setGeneratedEventUri(this.getGeneratedEventUri());
    e.setSerializationLangContentType(this.serializationLangContentType);
    e.setSerializationLangName(this.serializationLangName);
    e.setSerializedExplanationModel(this.serializedExplanationModel);
    return e;
  }

  @Override
  public String toString() {
    return "HintEvent: (" + getFromWonNodeUri() + ", " + getFromNeedUri() + ", " + getToWonNodeUri() + ", "
        + getToNeedUri() + ", " + getMatcherUri() + ", " + getScore() + ")";
  }
}