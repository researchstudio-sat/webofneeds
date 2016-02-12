package won.matcher.siren.matcher;

import com.hp.hpl.jena.vocabulary.DC;
import com.sindicetech.siren.qparser.tree.dsl.ConciseQueryBuilder;
import com.sindicetech.siren.qparser.tree.dsl.ConciseTwigQuery;
import com.sindicetech.siren.qparser.tree.dsl.TwigQuery;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import won.protocol.vocabulary.WON;

/**
 * Created by hfriedrich on 03.11.2015.
 */
public class SirenQueryBuilder
{
  private ConciseTwigQuery topTwig;
  private ConciseQueryBuilder builder;
  private int consideredQueryTokens;

  private static final String DEMAND = WON.BASIC_NEED_TYPE_DEMAND.toString().toLowerCase();
  private static final String SUPPLY = WON.BASIC_NEED_TYPE_SUPPLY.toString().toLowerCase();
  private static final String DOTOGETHER = WON.BASIC_NEED_TYPE_DO_TOGETHER.toString().toLowerCase();
  private static final String CRITIQUE = WON.BASIC_NEED_TYPE_CRITIQUE.toString().toLowerCase();

  public SirenQueryBuilder(NeedObject needObject, int consideredQueryTokens) throws QueryNodeException {

    this.consideredQueryTokens = consideredQueryTokens;
    builder = new ConciseQueryBuilder();
    topTwig = builder.newTwig("@graph");

    TwigQuery twigBasicNeedType = null;

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

    if (matchNeedType != null) {
      twigBasicNeedType = builder.newTwig(WON.HAS_BASIC_NEED_TYPE.toString()).with(
        builder.newNode("'" + matchNeedType + "'").setAttribute("@id"));
      
      topTwig.with(twigBasicNeedType);
    }
  }

  public void addTitleTerms(String[] terms) throws QueryNodeException {

    for (int i = 0; i < terms.length && i < consideredQueryTokens; i++) {

      // TODO: make the boosts configurable

      // search title terms in title
      topTwig.optional(builder.newTwig(WON.HAS_CONTENT.toString()).with(
        builder.newNode(terms[i]).setAttribute(DC.title.toString()).setBoost(3)));

      // search title terms also in description (tags will be search also, see addTagTerms())
      topTwig.optional(builder.newTwig(WON.HAS_CONTENT.toString()).with(
        builder.newNode(terms[i]).setAttribute(WON.HAS_TEXT_DESCRIPTION.toString())));
    }
  }

  public void addDescriptionTerms(String[] terms) throws QueryNodeException {

    for (int i = 0; i < terms.length && i < consideredQueryTokens; i++) {
      topTwig.optional(builder.newTwig(WON.HAS_CONTENT.toString()).with(
        builder.newNode(terms[i]).setAttribute(WON.HAS_TEXT_DESCRIPTION.toString())));
    }
  }

  public void addTagTerms(String[] terms) throws QueryNodeException {

    for (int i = 0; i < terms.length && i < consideredQueryTokens; i++) {

      // TODO: make the boosts configurable

      // search tag terms in tags
      topTwig.optional(builder.newTwig(WON.HAS_CONTENT.toString()).with(
        builder.newNode(terms[i]).setAttribute(WON.HAS_TAG.toString()).setBoost(3)));

      // search tag terms also in title and description
      topTwig.optional(builder.newTwig(WON.HAS_CONTENT.toString()).with(
        builder.newNode(terms[i]).setAttribute(DC.title.toString()).setBoost(2)));
    }
  }

  public String build() {
   return topTwig.toString();
  }
}

