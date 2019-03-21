package won.matcher.solr.query.factory;

/**
 * Created by hfriedrich on 22.08.2016.
 */
public class MultiplicativeBoostQueryFactory extends SolrQueryFactory {
  private String boostQuery;

  public MultiplicativeBoostQueryFactory(String boostQuery) {
    this.boostQuery = boostQuery;
  }

  @Override
  protected String makeQueryString() {
    return "{!boost b=" + boostQuery + "}";
  }
}
