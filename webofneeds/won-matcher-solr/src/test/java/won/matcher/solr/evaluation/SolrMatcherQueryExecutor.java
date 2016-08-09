package won.matcher.solr.evaluation;

import com.hp.hpl.jena.query.Dataset;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.matcher.solr.config.SolrMatcherConfig;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.query.TestNeedQueryFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by hfriedrich on 08.08.2016.
 */
@Component
public class SolrMatcherQueryExecutor
{
  @Autowired
  private SolrMatcherConfig config;

  @Autowired
  private HintBuilder hintBuilder;

  private SolrClient solrClient;

  @PostConstruct
  public void init() {
    solrClient = new HttpSolrClient.Builder(config.getSolrServerUri()).build();
  }

  public List<String> computeMatchingNeeds(Dataset need) throws IOException, SolrServerException {

    List<String> matchedNeeds = new LinkedList<>();
    SolrQuery query = new SolrQuery();
    query.setQuery(createQuery(need));
    query.setFields("id", "score",
                    "_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_dc_elements_1.1_title",
                    HintBuilder.WON_NODE_SOLR_FIELD);
    query.setRows(config.getMaxHints());

    QueryResponse response = solrClient.query(query);
    SolrDocumentList docs = response.getResults();
    SolrDocumentList newDocs = hintBuilder.calculateMatchingResults(docs);
    for (SolrDocument doc : newDocs) {
      String matchedNeedId = doc.getFieldValue("id").toString();
      matchedNeeds.add(matchedNeedId);
    }

    return matchedNeeds;
  }

  private String createQuery(Dataset need) {

    TestNeedQueryFactory needQuery = new TestNeedQueryFactory(need);
    needQuery.addTermsToTitleQuery(needQuery.getTitleTerms(), 4);
    needQuery.addTermsToTitleQuery(needQuery.getTagTerms(), 2);
    needQuery.addTermsToTagQuery(needQuery.getTagTerms(), 4);
    needQuery.addTermsToTagQuery(needQuery.getTitleTerms(), 2);
    needQuery.addTermsToDescriptionQuery(needQuery.getTitleTerms(), 2);
    needQuery.addTermsToDescriptionQuery(needQuery.getTagTerms(), 2);
    needQuery.addTermsToDescriptionQuery(needQuery.getDescriptionTerms(), 1);

    return needQuery.createQuery();
  }


}
