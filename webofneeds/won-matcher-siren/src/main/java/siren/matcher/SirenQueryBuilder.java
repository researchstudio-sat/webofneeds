package siren.matcher;

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

  public SirenQueryBuilder(NeedObject needObject, int consideredQueryTokens) throws QueryNodeException {

    this.consideredQueryTokens = consideredQueryTokens;
    builder = new ConciseQueryBuilder();
    topTwig = builder.newTwig("@graph");

    TwigQuery twigBasicNeedType = null;

    //First of all, we have to consider the BasicNeedType
    switch (needObject.getBasicNeedType().toLowerCase()) { //Attention: lower-case
      case "http://purl.org/webofneeds/model#supply": // Demands has to be matched
        twigBasicNeedType = builder.newTwig("http://purl.org/webofneeds/model#hasBasicNeedType").with(
          builder.newNode("'http://purl.org/webofneeds/model#demand'").setAttribute("@id"));
        break;
      case "http://purl.org/webofneeds/model#demand":
        twigBasicNeedType = builder.newTwig("http://purl.org/webofneeds/model#hasBasicNeedType").with(
          builder.newNode("'http://purl.org/webofneeds/model#supply'").setAttribute("@id"));
        break;
      case "http://purl.org/webofneeds/model#dotogether":
        twigBasicNeedType = builder.newTwig("http://purl.org/webofneeds/model#hasBasicNeedType").with(
          builder.newNode("'http://purl.org/webofneeds/model#dotogether'").setAttribute("@id"));
        break;
    }

    if (twigBasicNeedType != null) {
      topTwig.with(twigBasicNeedType);
    }
  }

  public void addTitleTerms(String[] terms) throws QueryNodeException {

    for (int i = 0; i < terms.length && i < consideredQueryTokens; i++) {
      topTwig.optional(builder.newTwig(WON.HAS_CONTENT.toString()).with(
        builder.newNode(terms[i]).setAttribute(DC.title.toString())));
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
      topTwig.optional(builder.newTwig(WON.HAS_CONTENT.toString()).with(
        builder.newNode(terms[i]).setAttribute(WON.HAS_TAG.toString())));
    }
  }

  public String build() {
   return topTwig.toString();
  }
}

