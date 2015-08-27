package common.event;

import java.io.Serializable;

/**
 * User: hfriedrich
 * Date: 23.06.2015
 */
public class HintEvent implements Serializable
{
  private String fromNeedUri;
  private String fromWonNodeUri;
  private String toNeedUri;
  private String toWonNodeUri;
  private double score;
  private String serializedExplanationGraph;
  private String serializationLangName;
  private String serializationLangContentType;

  public HintEvent(String fromWonNodeUri, String fromNeedUri, String toWonNodeUri, String toNeedUri, double score) {

    this.fromWonNodeUri = fromWonNodeUri;
    this.fromNeedUri = fromNeedUri;
    this.toWonNodeUri = toWonNodeUri;
    this.toNeedUri = toNeedUri;
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

  public double getScore() {
    return score;
  }
}