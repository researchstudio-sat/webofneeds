package won.matcher.solr.query;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.vocabulary.DC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.util.ArrayList;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class BasicNeedQueryFactory extends NeedDatasetQueryFactory
{
  private static final String NEED_TITLE_SOLR_FIELD =
    "_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_dc_elements_1.1_title";
  private static final String NEED_DESCRIPTION_SOLR_FIELD =
    "_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_webofneeds_model_hasTextDescription";
  private static final String NEED_TAG_SOLR_FIELD =
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
      titleTerms = titleTerms.replaceAll("[^A-Za-z0-9 ]", " ");

    } catch (IncorrectPropertyCountException e) {
      log.warn("Title not found in RDF dataset: " + e.toString());
    }

    try {
      descriptionTerms = RdfUtils.findOnePropertyFromResource(
        need, null, WON.HAS_TEXT_DESCRIPTION).asLiteral().getString();
      descriptionTerms = descriptionTerms.replaceAll("[^A-Za-z0-9 ]", " ");
    } catch (IncorrectPropertyCountException e) {
      log.warn("Description not found in RDF dataset: " + e.toString());
    }

    try {
      tagTerms = RdfUtils.findOnePropertyFromResource(need, null, WON.HAS_TAG).asLiteral().getString();
      tagTerms = tagTerms.replaceAll("[^A-Za-z0-9 ]", " ");
    } catch (IncorrectPropertyCountException e) {
      log.debug("Tags not found in RDF dataset: " + e.toString());
    }
  }

  public void addTermsToTitleQuery(String terms, double boost) {

    if (terms != null && terms != "") {
      SolrQueryFactory qf = new MatchFieldQuery(NEED_TITLE_SOLR_FIELD, terms);
      qf.setBoost(boost);
      contentFactories.add(qf);
    }
  }

  public void addTermsToDescriptionQuery(String terms, double boost) {

    if (terms != null && terms != "") {
      SolrQueryFactory qf = new MatchFieldQuery(NEED_DESCRIPTION_SOLR_FIELD, terms);
      qf.setBoost(boost);
      contentFactories.add(qf);
    }
  }

  public void addTermsToTagQuery(String terms, double boost) {

    if (terms != null && terms != "") {
      SolrQueryFactory qf = new MatchFieldQuery(NEED_TAG_SOLR_FIELD, terms);
      qf.setBoost(boost);
      contentFactories.add(qf);
    }
  }

  @Override
  protected String makeQueryString() {

    SolrQueryFactory[] factoryArray = new SolrQueryFactory[contentFactories.size()];
    BooleanQueryFactory contentShouldQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR, contentFactories
      .toArray(factoryArray));

    BooleanQueryFactory mustQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.AND, new
      NeedTypeQueryFactory(needDataset), new NeedStateQueryFactory(needDataset));

    BooleanQueryFactory topQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.AND, mustQuery,
                                                           contentShouldQuery);

    return topQuery.createQuery();
  }
}
