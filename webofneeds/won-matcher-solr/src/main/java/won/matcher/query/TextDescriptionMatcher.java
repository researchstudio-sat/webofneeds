package won.matcher.query;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.rest.LinkedDataRestClient;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 21.02.13
 * Time: 15:38
 * To change this template use File | Settings | File Templates.
 */
public class TextDescriptionMatcher {
    Logger logger;
    SolrCore solrCore;
    MatcherProtocolNeedServiceClient client;

    public TextDescriptionMatcher(SolrCore solrCore) {
        this.solrCore = solrCore;
        logger = LoggerFactory.getLogger(TextDescriptionMatcher.class);
        client = new MatcherProtocolNeedServiceClient();
        client.setLinkedDataRestClient(new LinkedDataRestClient());
    }

    public TextDescriptionMatcher() {
        this(null);
    }

    public void executeQuery() {
        logger.info("executeQuery called!!");
        //TODO: Deprecated.
        //if(SolrCore.openHandles.keySet().isEmpty())
        //    return;
        //solrCore = SolrCore.openHandles.keySet().iterator().next();
        solrCore = SolrCore.getSolrCore();
        SolrIndexSearcher si = solrCore.getSearcher().get();
        IndexReader ir = si.getIndexReader();

        MoreLikeThis mlt = new MoreLikeThis(ir);
        mlt.setMinDocFreq(1);
        mlt.setMinTermFreq(1);
        mlt.setMinWordLen(1);
        mlt.setFieldNames(new String[]{"ntriple"});
        Query query = null;
        TopDocs tdocs = null;

        logger.info("maxDoc: " + si.maxDoc());
            for(int i = 0; i < si.maxDoc(); i++) {
                try {
                    query = mlt.like(i);
                    //logger.info("doc:" + ir.document(i).get("url"));
                    //TODO: improve search request
                    tdocs = si.search(query, 1);
                    if(tdocs.totalHits > 0) {
                        //logger.info("MaxScore: " + tdocs.getMaxScore());
                        //logger.info("Field-price: " +  ir.document(tdocs.scoreDocs[0].doc).get("price"));
                        //logger.info("Field-ntriples: " +  ir.document(tdocs.scoreDocs[0].doc).get("ntriple"));

                        try {
                            URI fromURI = new URI(ir.document(i).get("url"));
                            URI toURI = new URI(ir.document(tdocs.scoreDocs[0].doc).get("url"));
                            logger.info("Match: " + fromURI + " to " + toURI + ", score: " + tdocs.getMaxScore());
                            client.hint(fromURI, toURI, tdocs.scoreDocs[0].score,  new URI("http://LDSpiderMatcher.webofneeds"));
                        } catch (NoSuchNeedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (IllegalMessageForNeedStateException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (URISyntaxException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }

                    } else {
                        logger.info("Not found!!");
                    }
                } catch (IOException e) {
                    logger.warn("Could not generate similarity query with document " + i + "!", e);  //To change body of catch statement use File | Settings | File Templates.
                }
            }

        // now the usual iteration thru 'hits' - the only thing to watch for is to make sure
        //you ignore the doc if it matches your 'target' document, as it should be similar to itself
    }
}
