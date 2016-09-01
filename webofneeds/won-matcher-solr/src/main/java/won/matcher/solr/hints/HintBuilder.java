package won.matcher.solr.hints;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.solr.config.SolrMatcherConfig;
import won.matcher.solr.utils.Kneedle;

import java.util.Comparator;
import java.util.List;

/**
 * Created by hfriedrich on 02.08.2016.
 */
@Component
public class HintBuilder
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  public final static String WON_NODE_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_hasWonNode._id";

  @Autowired
  private SolrMatcherConfig config;

  public SolrDocumentList calculateMatchingResults(final SolrDocumentList docs) {

    SolrDocumentList matches = new SolrDocumentList();
    if (docs == null || docs.size() == 0) {
      return matches;
    }

    if (log.isDebugEnabled()) {
      for (SolrDocument doc : docs) {
        String needUri = doc.getFieldValue("id").toString();
        double score = Double.valueOf(doc.getFieldValue("score").toString());
        log.debug("retrieved match {} from Solr score {}: ", needUri, score);
      }
    }

      // sort the documents according to their score value descending
      SolrDocumentList sortedDocs = (SolrDocumentList) docs.clone();
      sortedDocs.sort(new Comparator<SolrDocument>()
      {
        @Override
        public int compare(final SolrDocument o1, final SolrDocument o2) {
          if ((float) o1.getFieldValue("score") < (float) o2.getFieldValue("score"))
            return -1;
          else if ((float) o1.getFieldValue("score") > (float) o2.getFieldValue("score"))
            return 1;
          else
            return 0;
        }
      });

      // apply the Kneedle algorithm to find knee/elbow points in the score values of the returned docs to cut there
      double cutScoreLowerThan = 0.0;
      if (sortedDocs.size() > 1) {
        Kneedle kneedle = new Kneedle();
        double[] x = new double[sortedDocs.size()];
        double[] y = new double[sortedDocs.size()];
        for (int i = 0; i < sortedDocs.size(); i++) {
          x[i] = i;
          y[i] = Double.valueOf(sortedDocs.get(i).getFieldValue("score").toString());
        }
        int[] elbows = kneedle.detectElbowPoints(x, y);

        if (elbows.length >= config.getCutAfterIthElbowInScore()) {
          cutScoreLowerThan = y[elbows[elbows.length - config.getCutAfterIthElbowInScore()]];
          log.debug("Calculated elbow score point after {} elbows for document scores: {}",
                    config.getCutAfterIthElbowInScore(), cutScoreLowerThan);
        }
      }

      for (int i = sortedDocs.size() - 1; i >= 0; i--) {

        // if score is lower threshold or we arrived at the elbow point to cut after
        double score = Double.valueOf(sortedDocs.get(i).getFieldValue("score").toString());
        if (score < config.getScoreThreshold() || score <= cutScoreLowerThan) {
          log.debug("cut result documents, current score is {}, score threshold is {}",
                    score, config.getScoreThreshold());
          break;
        }

        SolrDocument newDoc = sortedDocs.get(i);
        matches.add(newDoc);
      }

      return matches;
  }

  public BulkHintEvent generateHintsFromSearchResult(final SolrDocumentList docs, final NeedEvent need) {

    // generate hints from query result documents
    BulkHintEvent bulkHintEvent = new BulkHintEvent();
    log.info("Received {} matches as query result for need {}", (docs != null) ? docs.size() : 0, need);
    SolrDocumentList newDocs = calculateMatchingResults(docs);
    log.info("Cut down result to {} matches for need query result {}", newDocs.size(), need);

    for (SolrDocument doc : newDocs) {

      String needUri = doc.getFieldValue("id").toString();
      String wonNodeUri = ((List) doc.getFieldValue(WON_NODE_SOLR_FIELD)).get(0).toString();

      // normalize the final score
      double score = Double.valueOf(doc.getFieldValue("score").toString()) * config.getScoreNormalizationFactor();

      if (score > 1.0) {
        score = 1.0;
      } else if (score < 0.0) {
        score = 0.0;
      }

      log.debug("generate hint for match {} with normalized score {}", needUri, score);
      bulkHintEvent.addHintEvent(new HintEvent(need.getWonNodeUri(), need.getUri(), wonNodeUri, needUri,
                                               config.getSolrServerPublicUri(), score));

      // also send the same hints to the other side (remote need and wonnode)?
      if (config.isCreateHintsForBothNeeds()) {
        bulkHintEvent.addHintEvent(new HintEvent(wonNodeUri, needUri, need.getWonNodeUri(), need.getUri(),
                                                 config.getSolrServerPublicUri(), score));
      }
    }

    return bulkHintEvent;
  }
}
