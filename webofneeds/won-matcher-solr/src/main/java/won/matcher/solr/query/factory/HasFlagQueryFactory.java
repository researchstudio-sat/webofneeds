package won.matcher.solr.query.factory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hfriedrich on 29.08.2017.
 */
public class HasFlagQueryFactory extends SolrQueryFactory {

  public enum FLAGS {
    WHATS_AROUND, WHATS_NEW, NO_HINT_FOR_COUNTERPART, NO_HINT_FOR_ME
  }

  private static final Map<FLAGS, String> flagMap;
  static {
    Map<FLAGS, String> myMap = new HashMap<>();
    myMap.put(FLAGS.WHATS_AROUND, "http\\://purl.org/webofneeds/model#WhatsAround");
    myMap.put(FLAGS.WHATS_NEW, "http\\://purl.org/webofneeds/model#WhatsNew");
    myMap.put(FLAGS.NO_HINT_FOR_COUNTERPART, "http\\://purl.org/webofneeds/model#NoHintForCounterpart");
    myMap.put(FLAGS.NO_HINT_FOR_ME, "http\\://purl.org/webofneeds/model#NoHintForMe");
    flagMap = Collections.unmodifiableMap(myMap);
  }

  private FLAGS flag;

  public HasFlagQueryFactory(FLAGS flag) {
    this.flag = flag;
  }

  @Override
  protected String makeQueryString() {
    return new ExactMatchFieldQueryFactory("_graph.http___purl.org_webofneeds_model_hasFlag._id", flagMap.get(flag))
        .createQuery();
  }
}
