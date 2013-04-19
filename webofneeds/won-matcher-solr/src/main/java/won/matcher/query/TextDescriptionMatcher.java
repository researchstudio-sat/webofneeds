package won.matcher.query;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.rest.LinkedDataRestClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

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
    private Set<String> knownMatches = new HashSet();

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
        mlt.setFieldNames(new String[]{"ntriple"});
        Query query = null;
        TopDocs tdocs = null;

        logger.info("maxDoc: " + si.maxDoc());
            for(int i = 0; i < si.maxDoc(); i++) {
                try {
                    query = mlt.like(i);
                  String fromUriString = ir.document(i).get("url");
                    //logger.info("doc:" + ir.document(i).get("url"));
                    //TODO: improve search request
                    tdocs = si.search(query,10);
                    if(tdocs.totalHits > 0) {
                        //logger.info("MaxScore: " + tdocs.getMaxScore());
                        //logger.info("Field-price: " +  ir.document(tdocs.scoreDocs[0].doc).get("price"));
                        //logger.info("Field-ntriples: " +  ir.document(tdocs.scoreDocs[0].doc).get("ntriple"));

                        try {

                            fromUriString = fromUriString.replaceAll("^<","").replaceAll(">$","");
                            URI fromURI = new URI(fromUriString);

                            String toUriString = ir.document(tdocs.scoreDocs[0].doc).get("url");
                            toUriString = toUriString.replaceAll("^<","").replaceAll(">$","");
                            URI toURI = new URI(toUriString);
                            if (fromURI.equals(toURI)) continue;

                            double score = tdocs.scoreDocs[0].score / 1000; //TODO fix this hack! how do we keep the score in (0,1)?
                            String matchKey = fromURI.toString() + " <=> " + toURI.toString();
                            String matchKey2 = toURI.toString() + " <=> " + fromURI.toString();
                            if (this.knownMatches.contains(matchKey) || this.knownMatches.contains(matchKey2)){
                              logger.info("ignoring known match: " + matchKey);
                            } else {
                              //add the match key before sending the hint!
                              this.knownMatches.add(matchKey);
                              logger.info("new match: " + matchKey);
                              logger.info("sending hint..");
                              client.hint(fromURI, toURI, score,  new URI("http://LDSpiderMatcher.webofneeds"));
                              client.hint(toURI, fromURI, score,  new URI("http://LDSpiderMatcher.webofneeds"));
                              logger.info("hint sent.");
                            }
                        } catch (NoSuchNeedException e) {
                            logger.warn("Hint failed: no such need", e);
                        } catch (IllegalMessageForNeedStateException e) {
                            logger.warn("Hint failed: illegal message for need state", e);
                        } catch (URISyntaxException e) {
                            logger.warn("Hint failed: illegal URI", e);
                        } catch (Exception e){
                          logger.warn("Hint failed.", e);
                        }

                    } else {
                        logger.info("Nothing found similar to " + fromUriString );
                    }
                } catch (IOException e) {
                    logger.warn("Could not generate similarity query with document " + i + "!", e);  //To change body of catch statement use File | Settings | File Templates.
                }
            }

        // now the usual iteration thru 'hits' - the only thing to watch for is to make sure
        //you ignore the doc if it matches your 'target' document, as it should be similar to itself
    }
}
