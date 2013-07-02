package won.matcher.query;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import javafx.util.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: atus
 * Date: 28.06.13
 */
public class QueryBase
{
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private MatcherProtocolNeedServiceClient client;

  private SolrCore solrCore;

  private Set<String> knownMatches;

  private Set<AbstractExtendedQuery> queries;

  protected static final String FIELD_NTRIPLE = "ntriple";
  private static final String FIELD_URL = "url";

  private static final int MAX_MATCHES = 3;
  private static final double MATCH_THRESHOLD = 0.1;
  private static final long TIMEOUT_BETWEEN_SEARHCES = 1000; //timeout in millis

  public QueryBase(SolrCore solrCore)
  {
    CoreContainer coreContainer = new CoreContainer();
    this.solrCore = coreContainer.getCore("webofneeds");

    client = new MatcherProtocolNeedServiceClient();
    client.setLinkedDataRestClient(new LinkedDataRestClient());

    queries = new HashSet<AbstractExtendedQuery>();

    knownMatches = new HashSet();
  }

  public void executeQuery()
  {
    //get last commited document

    //get set of documents to compare to (filter + more like this)

    //compare and send hints for best match(es)
  }

  public boolean checkMatchExists(String uri1, String uri2) {
    return knownMatches.contains(uri1 + " <=> " + uri2);
  }

  public void insertNewMatch(String uri1, String uri2) {
    knownMatches.add(uri1 + " <=> " + uri2);
    knownMatches.add(uri2 + " <=> " + uri1);
  }

}
