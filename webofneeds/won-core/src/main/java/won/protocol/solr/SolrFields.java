package won.protocol.solr;

/**
 * Constants required for use with our solr schema.
 * TODO [REFACTORING]: move to a solr-specific module containing the client code required to write to the solr index
 */
public class SolrFields
{
  public static final String URL = "url";

  public static final String NTRIPLE = "ntriple";

  public static final String TITLE = "title";
  public static final String DESCRIPTION = "description";

  public static final String BASIC_NEED_TYPE = "basicNeedType";

  public static final String LOCATION = "location";

  public static final String PRICE = "price";

  public static final String DURATION = "duration";

  public static final String TAG = "tag";

  public static final String KEYWORD_SEARCH = "keywordsearch";
}
