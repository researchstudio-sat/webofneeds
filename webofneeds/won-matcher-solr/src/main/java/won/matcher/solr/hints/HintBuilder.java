package won.matcher.solr.hints;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
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
import won.matcher.solr.query.factory.MatchingContextQueryFactory;
import won.matcher.solr.utils.Kneedle;
import won.protocol.util.NeedModelWrapper;
import won.protocol.vocabulary.WON;

/**
 * Created by hfriedrich on 02.08.2016.
 */
@Component
public class HintBuilder {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public final static String WON_NODE_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_hasWonNode._id";
    public final static String HAS_FLAG_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_hasFlag._id";

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
        sortedDocs.sort(new Comparator<SolrDocument>() {
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
                log.debug("cut result documents, current score is {}, score threshold is {}", score,
                        config.getScoreThreshold());
                break;
            }

            SolrDocument newDoc = sortedDocs.get(i);
            matches.add(newDoc);
        }

        return matches;
    }

    public BulkHintEvent generateHintsFromSearchResult(final SolrDocumentList docs, final NeedEvent need,
            NeedModelWrapper needModelWrapper, boolean doSuppressHintForNeed, boolean doSuppressHintForMatchedNeeds,
            boolean kneeDetection) {

        // check if knee detection should be performed
        SolrDocumentList newDocs = docs;
        if (kneeDetection) {
            newDocs = calculateMatchingResults(docs);
        }

        BulkHintEvent bulkHintEvent = new BulkHintEvent();
        log.info("Received {} matches as query result for need {}, keeping the top {} ",
                new Object[] { (docs != null) ? docs.size() : 0, need, newDocs.size() });
        boolean noHintForMe = needModelWrapper.hasFlag(WON.NO_HINT_FOR_ME);
        boolean noHintForCounterpart = needModelWrapper.hasFlag(WON.NO_HINT_FOR_COUNTERPART);
        log.debug("need to be matched has NoHintForMe: {}, NoHintForCounterpart: {} ", noHintForMe,
                noHintForCounterpart);
        for (SolrDocument doc : newDocs) {
            // NOTE: not the whole document is loaded here. The fields that are selected are defined
            // in won.matcher.solr.query.DefaultMatcherQueryExecuter - if additional fields are required, the field list
            // has to be extended in that class.

            String matchedNeedUri = doc.getFieldValue("id").toString();
            if (matchedNeedUri == null) {
                log.debug("omitting matched need: could not extract need URI");
                continue;
            }

            List<String> flags = getValueList(doc, HAS_FLAG_SOLR_FIELD);
            boolean matchedNeedNoHintForMe = flags.contains(WON.NO_HINT_FOR_ME.toString());
            boolean matchedNeedNoHintForCounterpart = flags.contains(WON.NO_HINT_FOR_COUNTERPART.toString());

            // check the matching contexts of the two needs that are supposed to be matched
            // send only hints to needs if their matching contexts overlap (if one need has empty matching context it
            // always receives hints)
            Collection<Object> contextSolrFieldValues = doc
                    .getFieldValues(MatchingContextQueryFactory.MATCHING_CONTEXT_SOLR_FIELD);
            Collection<String> matchedNeedMatchingContexts = new LinkedList<>();
            if (contextSolrFieldValues != null) {
                matchedNeedMatchingContexts = contextSolrFieldValues.stream().map(a -> (String) a)
                        .collect(Collectors.toList());
            }
            Collection<String> matchingContexts = needModelWrapper.getMatchingContexts();
            if (matchingContexts == null) {
                matchingContexts = new LinkedList<>();
            }
            boolean contextOverlap = CollectionUtils.intersection(matchedNeedMatchingContexts, matchingContexts)
                    .size() > 0;
            boolean suppressHintsForMyContexts = !contextOverlap && !(CollectionUtils.isEmpty(matchingContexts));
            boolean suppressHintsForCounterpartContexts = !contextOverlap
                    && !(CollectionUtils.isEmpty(matchedNeedMatchingContexts));

            // suppress hints for current if its flags or its counterparts flags say so or if it was specified in the
            // calling parameters or matching contexts
            doSuppressHintForNeed = noHintForMe || matchedNeedNoHintForCounterpart || doSuppressHintForNeed
                    || suppressHintsForMyContexts;

            // suppress hints for matched need if its flags or its counterparts flags say so or if it was specified in
            // the calling parameters or matching contexts
            doSuppressHintForMatchedNeeds = noHintForCounterpart || matchedNeedNoHintForMe
                    || doSuppressHintForMatchedNeeds || suppressHintsForCounterpartContexts;

            if (log.isDebugEnabled()) {
                log.debug("matched need has NoHintForMe: {}, NoHintForCounterpart: {}", matchedNeedNoHintForMe,
                        matchedNeedNoHintForCounterpart);
                log.debug("need will receive a hint: {} (uri: {})", !doSuppressHintForNeed, need.getUri());
                log.debug("matched need need will receive a hint: {} (uri: {})", !doSuppressHintForMatchedNeeds,
                        matchedNeedUri);
                log.debug("need matching contexts: {}", matchingContexts);
                log.debug("matched need matching contexts: {}", matchedNeedMatchingContexts);
            }
            if (doSuppressHintForNeed && doSuppressHintForMatchedNeeds) {
                log.debug("no hints to be sent because of Suppress settings");
                continue;
            }
            // wonNodeUri can be returned as either a String or ArrayList, not sure on what this depends
            String wonNodeUri = getFieldValueFirstOfListIfNecessary(doc, WON_NODE_SOLR_FIELD);
            if (wonNodeUri == null) {
                log.debug("omitting matched need {}: could not extract WoN node URI", matchedNeedUri);
                continue;
            }

            // normalize the final score
            double score = Double.valueOf(doc.getFieldValue("score").toString()) * config.getScoreNormalizationFactor();
            score = Math.max(0, Math.min(1, score));

            log.debug("generate hint for match {} with normalized score {}", matchedNeedUri, score);

            if (!doSuppressHintForNeed) {
                bulkHintEvent.addHintEvent(new HintEvent(need.getWonNodeUri(), need.getUri(), wonNodeUri,
                        matchedNeedUri, config.getSolrServerPublicUri(), score));
            }

            // also send the same hints to the other side (remote need and wonnode)?
            if (!doSuppressHintForMatchedNeeds) {
                bulkHintEvent.addHintEvent(new HintEvent(wonNodeUri, matchedNeedUri, need.getWonNodeUri(),
                        need.getUri(), config.getSolrServerPublicUri(), score));
            }
        }

        return bulkHintEvent;
    }

    private List<String> getValueList(SolrDocument document, String fieldName) {
        Object value = document.getFieldValue(fieldName);
        if (value == null)
            return Collections.emptyList();
        if (value instanceof String) {
            return Arrays.asList(new String[] { (String) value });
        }
        if (value instanceof List) {
            return ((List<String>) ((List) value).stream().map(x -> x.toString()).collect(Collectors.toList()));
        }
        return Collections.emptyList();
    }

    private String getFieldValueFirstOfListIfNecessary(SolrDocument doc, String field) {
        Object value = doc.getFieldValue(field);
        if (value == null)
            return null;
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            return ((List) value).get(0).toString();
        }
        return null;
    }

}
