package won.matcher.siren.matcher;

import com.hp.hpl.jena.vocabulary.DC;
import com.sindicetech.siren.qparser.tree.dsl.*;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

/**
 * Created by hfriedrich on 03.11.2015.
 */
public class SirenQueryBuilder
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private AbstractBooleanQuery topQuery;
  private ConciseQueryBuilder builder;
  private int consideredQueryTokens;
  private int usedQueryTokens;

  private static final String DEMAND = WON.BASIC_NEED_TYPE_DEMAND.toString().toLowerCase();
  private static final String SUPPLY = WON.BASIC_NEED_TYPE_SUPPLY.toString().toLowerCase();
  private static final String DOTOGETHER = WON.BASIC_NEED_TYPE_DO_TOGETHER.toString().toLowerCase();
  private static final String CRITIQUE = WON.BASIC_NEED_TYPE_CRITIQUE.toString().toLowerCase();

  public SirenQueryBuilder(NeedObject needObject, int consideredQueryTokens) throws QueryNodeException {

    this.consideredQueryTokens = consideredQueryTokens;
    usedQueryTokens = 0;
    builder = new ConciseQueryBuilder();
    topQuery =  builder.newBoolean();

    //First of all, we have to consider the BasicNeedType
    String matchNeedType = null;
    if (DEMAND.equals(needObject.getBasicNeedType().toLowerCase())) {
      matchNeedType = SUPPLY;
    } else if (SUPPLY.equals(needObject.getBasicNeedType().toLowerCase())) {
      matchNeedType = DEMAND;
    } else if (DOTOGETHER.equals(needObject.getBasicNeedType().toLowerCase())) {
      matchNeedType = DOTOGETHER;
    } else if (CRITIQUE.equals(needObject.getBasicNeedType().toLowerCase())) {
      matchNeedType = CRITIQUE;
    }

    TwigQuery twigBasicNeedType = builder.newTwig("@graph").with(builder.newTwig(WON.HAS_BASIC_NEED_TYPE.toString()).with(
      builder.newNode("'" + matchNeedType + "'").setAttribute("@id")));
    topQuery.with(twigBasicNeedType);

    // retrieve only needs in state active
    TwigQuery needState = builder.newTwig("@graph").with(builder.newTwig(WON.IS_IN_STATE.toString()).with(
      builder.newNode("'" + WON.NEED_STATE_ACTIVE + "'").setAttribute("@id")));
    topQuery.with(needState);
  }

  public void addTermsToTitleQuery(String[] terms, int boost)  throws QueryNodeException {
    addTermsToContentQuery(terms, DC.title.toString(), boost);
  }

  public void addTermsToDescriptionQuery(String[] terms, int boost)  throws QueryNodeException {
    addTermsToContentQuery(terms, WON.HAS_TEXT_DESCRIPTION.toString(), boost);
  }

  public void addTermsToTagQuery(String[] terms, int boost)  throws QueryNodeException {
    addTermsToContentQuery(terms, WON.HAS_TAG.toString(), boost);
  }

  public void addTermsToContentQuery(String[] terms, String contentAttribute, int boost) throws QueryNodeException {

    if (terms == null || terms.length == 0) {
      return;
    }

    if (usedQueryTokens + terms.length <= consideredQueryTokens) {
      String queryTerms = String.join(" OR ", terms);
      usedQueryTokens += terms.length;
      topQuery.optional(builder.newTwig("@graph").with(builder.newTwig(WON.HAS_CONTENT.toString()).with(
        builder.newNode(queryTerms).setAttribute(contentAttribute).setBoost(boost))));
    } else {
      log.warn("Cannot add more terms to the Solr query. Reached considered number of terms: " + consideredQueryTokens);
      return;
    }
  }

  public String build() {
   return topQuery.toString();
  }
}

