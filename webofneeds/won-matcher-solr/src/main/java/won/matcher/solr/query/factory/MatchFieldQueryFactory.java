package won.matcher.solr.query.factory;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class MatchFieldQueryFactory extends SolrQueryFactory {
  protected String fieldName;
  protected String value;

  public MatchFieldQueryFactory() {
    fieldName = null;
    value = null;
  }

  public MatchFieldQueryFactory(String fieldName, String value) {
    this.value = value;
    this.fieldName = fieldName;
  }

  @Override
  protected String makeQueryString() {

    if (fieldName == null || value == null) {
      throw new NullPointerException("fieldName or value may not be null");
    }
    // these surrounding brackets are important to really search for all terms (that
    // the value field may contain)
    // in the specified in the field
    return String.join("", fieldName, " : (", value, ")");
  }
}
