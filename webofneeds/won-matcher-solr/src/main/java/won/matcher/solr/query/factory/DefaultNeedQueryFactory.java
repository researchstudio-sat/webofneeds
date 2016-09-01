package won.matcher.solr.query.factory;

import com.hp.hpl.jena.query.Dataset;

/**
 * Created by hfriedrich on 03.08.2016.
 */
public class DefaultNeedQueryFactory extends BasicNeedQueryFactory
{
  public DefaultNeedQueryFactory(final Dataset need) {
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
