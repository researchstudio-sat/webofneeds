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

    public BulkHintEvent generateHintsFromSearchResult(SolrDocumentList docs, NeedEvent need) {

        if (docs == null || docs.size() == 0) {
            return new BulkHintEvent();
        } else {
            log.debug("Received {} solr documents as query result", docs.size());
        }

        // sort the documents according to their score value descending
        docs.sort(new Comparator<SolrDocument>()
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
        Kneedle kneedle = new Kneedle();
        double[] x = new double[docs.size() + 1];
        double[] y = new double[docs.size() + 1];
        for (int i = 0; i < docs.size(); i++) {
            x[i] = i;
            y[i] = Double.valueOf(docs.get(i).getFieldValue("score").toString());
        }

        // add one additional point at the end so that an elbow for the top element is found too
        x[docs.size()] = docs.size();
        y[docs.size()] = y[docs.size()-1];
        int[] elbows = kneedle.detectElbowPoints(x, y);
        double cutScoreLowerThan = 0.0;
        if (elbows.length >= config.getCutAfterIthElbowInScore()) {
            cutScoreLowerThan = y[elbows[elbows.length - config.getCutAfterIthElbowInScore()]] / docs.getMaxScore();
            log.debug("Calculated elbow score point after {} elbows for document scores: {}",
                      config.getCutAfterIthElbowInScore(), cutScoreLowerThan);
        }

        // generate hints from query result documents
        BulkHintEvent bulkHintEvent = new BulkHintEvent();
        for (int i = docs.size() - 1; i >= 0; i--) {

            String needUri = docs.get(i).getFieldValue("id").toString();
            String wonNodeUri = ((List) docs.get(i).getFieldValue(WON_NODE_SOLR_FIELD)).get(0).toString();

            // normalize the score to a value between 0 and 1
            double score = Double.valueOf(docs.get(i).getFieldValue("score").toString()) / docs.getMaxScore();

            // if score is lower threshold or we arrived at the elbow point to cut after
            if (score < config.getScoreThreshold() || score <= (cutScoreLowerThan)) {
                log.debug("cut result documents, current score is {}, score threshold is {}",
                          score, config.getScoreThreshold());
                break;
            }

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
