package won.matcher.solr;

import com.sun.corba.se.spi.servicecontext.SendingContextServiceContext;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.matcher.query.*;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.solr.SolrFields;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
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

  private HashMap<String, ScoreDoc[]> knownMatches;

  private Set<AbstractQuery> queries;

  private static final int MAX_MATCHES = 3;
  private static final double MATCH_THRESHOLD = 0.1;

  public MatcherRequestProcessor(UpdateRequestProcessor next)
  {
    super(next);
    logger.debug("Matcher initialize called");

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
    queries = new HashSet<AbstractQuery>();
    queries.add(new BasicNeedTypeQuery(BooleanClause.Occur.MUST, SolrFields.BASIC_NEED_TYPE));
//    queries.add(new IntegerRangeQuery(BooleanClause.Occur.SHOULD, SolrFields.LOWER_PRICE_LIMIT, SolrFields.UPPER_PRICE_LIMIT));
//    queries.add(new LongRangeQuery(BooleanClause.Occur.SHOULD, SolrFields.START_TIME, SolrFields.END_TIME));
//    queries.add(new TextMatcherQuery(BooleanClause.Occur.SHOULD, new String[]{SolrFields.TITLE, SolrFields.DESCRIPTION}));
    queries.add(new SpatialQuery(BooleanClause.Occur.MUST, SolrFields.LOCATION, solrCore));

    knownMatches = new HashMap();
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException
  {
    super.processAdd(cmd);
    logger.debug("Matcher add called");

    //init
    BooleanQuery linkedQueries = new BooleanQuery();
    TopDocs topDocs;

    //get last commited document
    SolrInputDocument inputDocument = cmd.getSolrInputDocument();

    //get index
    SolrIndexSearcher solrIndexSearcher = solrCore.getSearcher().get();

    //get set of documents to compare to (filter + more like this, etc)
    for (AbstractQuery query : queries) {
      Query q = query.getQuery(solrIndexSearcher, inputDocument);
      if (q != null)
        linkedQueries.add(q, query.getOccur());
    }

    //compare and select best match(es) for hints
    topDocs = solrIndexSearcher.search(linkedQueries, 10);


    if (topDocs.scoreDocs.length != 0)
      knownMatches.put(inputDocument.getFieldValue(SolrFields.URL).toString(), topDocs.scoreDocs);

    for (String match : knownMatches.keySet()) {
      StringBuilder sb = new StringBuilder();
      for (ScoreDoc score : knownMatches.get(match))
        sb.append(String.format("\ndocument: %s\tscore:%2.3f", score.doc, score.score));

      logger.info("document: " + match + " " + sb.toString());
    }
  }

  @Override
  public void finish() throws IOException
  {
    super.finish();
    logger.info("Matcher finish called.");
    //cleanup or send prepared matches

//    try {
    URI originator = URI.create("http://LDSpiderMatcher.webofneeds");

    IndexReader indexReader = solrCore.getSearcher().get().getIndexReader();

    for (String match : knownMatches.keySet()) {
      URI fromDoc = URI.create(match);
      for (ScoreDoc score : knownMatches.get(match)) {
        Document document = indexReader.document(score.doc);
        URI toDoc = URI.create(document.get(SolrFields.URL));
        log.info(String.format("Sending hint %s -> %s :: %3.2f",fromDoc,toDoc,score.score));
        //client.hint(fromDoc, toDoc, score.score, originator, null);
      }
    }
//    } catch (NoSuchNeedException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    } catch (IllegalMessageForNeedStateException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    }

  }
}
