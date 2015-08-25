package common.event;

import org.apache.jena.riot.Lang;

/**
 * User: hfriedrich
 * Date: 23.06.2015
 */
public class HintEvent
{
  private String fromNeedUri;
  private String toNeedUri;
  private double score;
  private String serializedExplanationGraph;
  private Lang serializationFormat;

  public HintEvent(String fromNeedUri, String toNeedUri, double score) {
    this.fromNeedUri = fromNeedUri;
    this.toNeedUri = toNeedUri;
    this.score = score;
  }

  public String getFromNeedUri() {
    return fromNeedUri;
  }

  public String getToNeedUri() {
    return toNeedUri;
  }

  public double getScore() {
    return score;
  }
}