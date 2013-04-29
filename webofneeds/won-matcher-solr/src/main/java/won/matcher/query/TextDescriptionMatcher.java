package won.matcher.query;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
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
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
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
    private static final String FIELD_NTRIPLE = "ntriple";
    private static final String FIELD_URL = "url";
    private static final int MAX_MATCHES = 3;

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
        mlt.setFieldNames(new String[]{FIELD_NTRIPLE});
        Query query = null;
        TopDocs tdocs = null;

        int numMatches = 0;

        logger.info("maxDoc: " + si.maxDoc());
            for(int i = 0; i < si.maxDoc(); i++) {
              try {
                  query = mlt.like(i);
              String fromUriString = ir.document(i).getValues(FIELD_URL)[0]; //getValues() instead of get() just to make sure we don't have more than 1

              tdocs = si.search(query,10);
              logger.info("Found {} hits for doc {}", tdocs.totalHits, ir.document(i).getValues(FIELD_URL)[0]);
              if(tdocs.totalHits > 0) {
                logger.info("MaxScore: {}", tdocs.getMaxScore());
                fromUriString = fromUriString.replaceAll("^<","").replaceAll(">$","");
                URI fromURI = URI.create(fromUriString);

                //now load the triples from the doc and make a model so we can perform some basic checks
                String fromNtriples = ir.document(i).get(FIELD_NTRIPLE);
                Model fromModel = convertNTriplesToModel(fromNtriples);
                //check if we are talking about a need here. If not, don't consider the matches
                if (!isNeed(fromURI.toString(), fromModel)) {
                  logger.info("not a need, skipping: {}", fromURI.toString());
                  continue;
                }
                //remember what need type it is so we can compare to the need type of what we find
                //TODO: move this check into the query
                Resource fromBasicNeedType = getBasicNeedType(fromUriString, fromModel);



                ScoreDoc[] scoreDocs = tdocs.scoreDocs;
                for (int docInd = 0; docInd < scoreDocs.length; docInd++) {
                  try {
                    Document toDoc = ir.document(tdocs.scoreDocs[docInd].doc);
                    String toUriString = toDoc.getValues(FIELD_URL)[0];
                    toUriString = toUriString.replaceAll("^<","").replaceAll(">$","");
                    URI toURI = new URI(toUriString);
                    if (fromURI.equals(toURI)) continue;

                    logger.info("from document url: {}",fromURI);
                    logger.info("to document url: {}",toURI);
                    //TODO fix this hack! how do we keep the score in (0,1)?
                    double score = tdocs.scoreDocs[docInd].score / 10;
                    score = Math.max(0,Math.min(1,score));
                    logger.info("score: {}", score);

                    //now load the triples from the doc and make a model so we can perform some basic checks
                    String toNtriples = toDoc.get(FIELD_NTRIPLE);
                    Model toModel = convertNTriplesToModel(toNtriples);
                    //check if we are talking about a need here. If not, don't consider the matches
                    if (!isNeed(toURI.toString(), toModel)) {
                      logger.info("not a need, skipping: {}", toURI.toString());
                      continue;
                    }
                    //remember what need type it is so we can compare to the need type of what we find
                    //TODO: move this check into the query
                    Resource toBasicNeedType = getBasicNeedType(toURI.toString(), toModel);

                    //ignore the match if the need types don't fit
                    if (!isCompatibleBasicNeedType(fromBasicNeedType, toBasicNeedType)){
                      logger.info("basic need types incompatible (from:{}, to:{}). Ignoring match {}", new Object[]{ fromBasicNeedType, toBasicNeedType, toURI.toString()});
                      continue;
                    }



                    String matchKey = fromURI.toString() + " <=> " + toURI.toString();
                    String matchKey2 = toURI.toString() + " <=> " + fromURI.toString();
                    if (this.knownMatches.contains(matchKey) || this.knownMatches.contains(matchKey2)){
                      logger.info("ignoring known match: " + matchKey);
                      //if we send one match, that's enough - but it seems we've sent it already, so break
                      break;
                    } else {
                      //add the match key before sending the hint!
                      this.knownMatches.add(matchKey);
                      logger.info("new match: " + matchKey);
                      logger.info("sending hint..");
                      client.hint(fromURI, toURI, score,  new URI("http://LDSpiderMatcher.webofneeds"));
                      client.hint(toURI, fromURI, score,  new URI("http://LDSpiderMatcher.webofneeds"));
                      logger.info("hint sent.");
                      //check if we have enough matches
                      if (++numMatches > MAX_MATCHES) break;
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

                }

              } else {
                logger.info("Nothing found similar to " + fromUriString );
              }
            } catch (IOException e) {
        logger.warn("Could not generate similarity query with document " + i + "!", e);  //To change body of catch statement use File | Settings | File Templates.
      }
            }

        logger.info("done matching. Known matches: \n {}", Arrays.toString(knownMatches.toArray(new String[knownMatches.size()])));

    }

    private Model convertNTriplesToModel(String ntriples){
      Model model = ModelFactory.createDefaultModel();
      model.read(new StringReader(ntriples), WON.getURI(), "N-TRIPLES");
      return model;
    }

    private boolean isNeed(String needURI, Model model){
      return model.contains(model.getResource(needURI), RDF.type, WON.NEED);
    }

    private Resource getBasicNeedType(String needURI, Model model){
      Statement stmt = model.getProperty(model.getResource(needURI), WON.HAS_BASIC_NEED_TYPE);
      if (stmt == null) {
        return null;
      }
      return stmt.getObject().asResource();
    }

    private boolean isCompatibleBasicNeedType(Resource fromType, Resource toType){
      return WON.BASIC_NEED_TYPE_DO.equals(fromType) && WON.BASIC_NEED_TYPE_DO.equals(toType) ||
          WON.BASIC_NEED_TYPE_GIVE.equals(fromType) && WON.BASIC_NEED_TYPE_TAKE.equals(toType) ||
          WON.BASIC_NEED_TYPE_TAKE.equals(fromType) && WON.BASIC_NEED_TYPE_GIVE.equals(toType);
    }

}
