package won.matcher.solr;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.update.*;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.matcher.query.AbstractExtendedQuery;
import won.matcher.query.IntegerRangeFilterQuery;
import won.protocol.rest.LinkedDataRestClient;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * User: atus
 * Date: 03.07.13
 */
public class MatcherRequestProcessor extends UpdateRequestProcessor
{

  private Logger logger = LoggerFactory.getLogger(getClass());

  private MatcherProtocolNeedServiceClient client;

  private SolrCore solrCore;

  private Set<String> knownMatches;

  private Set<AbstractExtendedQuery> queries;

  protected static final String FIELD_NTRIPLE = "ntriple";
  private static final String FIELD_URL = "url";

  private static final int MAX_MATCHES = 3;
  private static final double MATCH_THRESHOLD = 0.1;
  private static final long TIMEOUT_BETWEEN_SEARHCES = 1000; //timeout in millis

  public MatcherRequestProcessor(UpdateRequestProcessor next)
  {
    super(next);
    logger.info("MATCHER INITIALIZED.");

    //get solr core
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer = null;
    try {
      coreContainer = initializer.initialize();
    } catch (Exception e) {
      logger.error("Failed to initialize core container. Stopping.", e);
      return;
    }
    solrCore = coreContainer.getCore("webofneeds");

    //setup matcher client
    client = new MatcherProtocolNeedServiceClient();
    client.initializeDefault();

    //add all queries
    queries = new HashSet<AbstractExtendedQuery>();

    knownMatches = new HashSet();
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException
  {
    super.processAdd(cmd);
    logger.info("MATCHER ADD");

    //get last commited document
    SolrInputDocument inputDocument = cmd.getSolrInputDocument();

    //get set of documents to compare to (filter + more like this, etc)

    //compare and select best match(es) for hints
  }

  @Override
  public void finish() throws IOException
  {
    super.finish();
    logger.info("MATCHER FINISH");

    //cleanup or send prepared matches
  }

  public boolean checkMatchExists(String uri1, String uri2)
  {
    return knownMatches.contains(uri1 + " <=> " + uri2);
  }

  public void insertNewMatch(String uri1, String uri2)
  {
    knownMatches.add(uri1 + " <=> " + uri2);
    knownMatches.add(uri2 + " <=> " + uri1);
  }
}
