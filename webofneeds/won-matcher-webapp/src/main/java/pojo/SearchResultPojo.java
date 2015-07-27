package pojo;

/**
 * User: syim
 * Date: 19.01.2015
 */
public class SearchResultPojo
{
  private String matchURI;
  private float score;

  public SearchResultPojo(){

  }
  public SearchResultPojo(String matchURI, float score){
    this.matchURI = matchURI;
    this.score = score;
  }

  public String getMatchURI() {
    return matchURI;
  }

  public void setMatchURI(final String matchURI) {
    this.matchURI = matchURI;
  }

  public double getScore() {
    return score;
  }

  public void setScore(final float score) {
    this.score = score;
  }
}
