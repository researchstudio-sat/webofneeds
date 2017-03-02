package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;

/**
 * Created by hfriedrich on 03.08.2016.
 */
public class TestNeedQueryFactory extends BasicNeedQueryFactory
{
  public TestNeedQueryFactory(final Dataset need) {
    super(need);

    addTermsToTitleQuery(getTitleTerms(), 4);
    addTermsToTitleQuery(getTagTerms(), 2);
    addTermsToTagQuery(getTagTerms(), 4);
    addTermsToTagQuery(getTitleTerms(), 2);
    addTermsToDescriptionQuery(getTitleTerms(), 2);
    addTermsToDescriptionQuery(getTagTerms(), 2);
    addTermsToDescriptionQuery(getDescriptionTerms(), 1);
  }
}

