package won.matcher.solr.query.factory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.vocabulary.DC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

import java.util.ArrayList;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class BasicNeedQueryFactory extends NeedDatasetQueryFactory
{
  public static final String NEED_TITLE_SOLR_FIELD =
    "_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_dc_elements_1.1_title";
  public static final String NEED_DESCRIPTION_SOLR_FIELD =
    "_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_webofneeds_model_hasTextDescription";
  public static final String NEED_TAG_SOLR_FIELD =
    "_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_webofneeds_model_hasTag";

  private final Logger log = LoggerFactory.getLogger(getClass());
  protected ArrayList<SolrQueryFactory> contentFactories;
  private String titleTerms;
  private String descriptionTerms;
  private String tagTerms;

  public String getTitleTerms() {
    return titleTerms;
  }

  public String getDescriptionTerms() {
    return descriptionTerms;
  }

  public String getTagTerms() {
    return tagTerms;
  }

  public BasicNeedQueryFactory(final Dataset need) {
    super(need);

    contentFactories =  new ArrayList<>();
    try {
      titleTerms = RdfUtils.findOnePropertyFromResource(need, null, DC.title).asLiteral().getString();
      titleTerms = filterCharsAndKeyWords(titleTerms);

    } catch (IncorrectPropertyCountException e) {
      log.warn("Title not found in RDF dataset: " + e.toString());
    }

    try {
      descriptionTerms = RdfUtils.findOnePropertyFromResource(
        need, null, WON.HAS_TEXT_DESCRIPTION).asLiteral().getString();
      descriptionTerms = filterCharsAndKeyWords(descriptionTerms);
    } catch (IncorrectPropertyCountException e) {
      log.warn("Description not found in RDF dataset: " + e.toString());
    }

    try {
      tagTerms = "\"" + String.join("\" \"", WonRdfUtils.NeedUtils.getTags(need)) + "\"";
    } catch (IncorrectPropertyCountException e) {
      log.debug("Tags not found in RDF dataset: " + e.toString());
    }
  }

  public void addTermsToTitleQuery(String terms, double boost) {

    if (terms != null && !terms.trim().isEmpty()) {
      SolrQueryFactory qf = new MatchFieldQueryFactory(NEED_TITLE_SOLR_FIELD, terms);
      qf.setBoost(boost);
      contentFactories.add(qf);
    }
  }

  public void addTermsToDescriptionQuery(String terms, double boost) {

    if (terms != null && !terms.trim().isEmpty()) {
      SolrQueryFactory qf = new MatchFieldQueryFactory(NEED_DESCRIPTION_SOLR_FIELD, terms);
      qf.setBoost(boost);
      contentFactories.add(qf);
    }
  }

  public void addTermsToTagQuery(String terms, double boost) {

    if (terms != null && !terms.trim().isEmpty()) {
      SolrQueryFactory qf = new MatchFieldQueryFactory(NEED_TAG_SOLR_FIELD, terms);
      qf.setBoost(boost);
      contentFactories.add(qf);
    }
  }

  private String filterCharsAndKeyWords(String text) {

    // filter all special characters and number
    text = text.replaceAll("[^A-Za-z ]", " ");
    text = text.replaceAll("[^A-Za-z ]", " ");
    text = text.replaceAll("NOT", " ");
    text = text.replaceAll("AND", " ");
    text = text.replaceAll("OR", " ");
    text = text.replaceAll("\\s+", " ");
    return text;
  }

  @Override
  protected String makeQueryString() {

    // add the term queries of the title, description and tags fields
    SolrQueryFactory[] factoryArray = new SolrQueryFactory[contentFactories.size()];
    BooleanQueryFactory contentQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR,
                                                               contentFactories.toArray(factoryArray));

    // add a multiplicative boost for the closer geographical distances
    return new GeoDistBoostQueryFactory(needDataset).createQuery() + contentQuery.createQuery();
  }
}
