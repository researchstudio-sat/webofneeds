package won.matcher.solr.hints;

import org.apache.commons.collections.CollectionUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.event.AtomHintEvent;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.solr.config.SolrMatcherConfig;
import won.matcher.solr.query.factory.MatchingContextQueryFactory;
import won.matcher.solr.utils.Katomle;
import won.protocol.util.AtomModelWrapper;
import won.protocol.vocabulary.WONMATCH;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by hfriedrich on 02.08.2016.
 */
@Component
public class HintBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public final static String WON_NODE_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_wonNode._id";
    public final static String HAS_FLAG_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_flag._id";
    @Autowired
    private SolrMatcherConfig config;

    public SolrDocumentList calculateMatchingResults(final SolrDocumentList docs) {
        SolrDocumentList matches = new SolrDocumentList();
        if (docs == null || docs.size() == 0) {
            return matches;
        }
        if (logger.isDebugEnabled()) {
            for (SolrDocument doc : docs) {
                String atomUri = doc.getFieldValue("id").toString();
                double score = Double.valueOf(doc.getFieldValue("score").toString());
                logger.debug("retrieved match {} from Solr score {}: ", atomUri, score);
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
        // apply the Katomle algorithm to find knee/elbow points in the score values of
        // the returned docs to cut there
        double cutScoreLowerThan = 0.0;
        if (sortedDocs.size() > 1) {
            Katomle katomle = new Katomle();
            double[] x = new double[sortedDocs.size()];
            double[] y = new double[sortedDocs.size()];
            for (int i = 0; i < sortedDocs.size(); i++) {
                x[i] = i;
                y[i] = Double.valueOf(sortedDocs.get(i).getFieldValue("score").toString());
            }
            int[] elbows = katomle.detectElbowPoints(x, y);
            if (elbows.length >= config.getCutAfterIthElbowInScore()) {
                cutScoreLowerThan = y[elbows[elbows.length - config.getCutAfterIthElbowInScore()]];
                logger.debug("Calculated elbow score point after {} elbows for document scores: {}",
                                config.getCutAfterIthElbowInScore(), cutScoreLowerThan);
            }
        }
        for (int i = sortedDocs.size() - 1; i >= 0; i--) {
            // if score is lower threshold or we arrived at the elbow point to cut after
            double score = Double.valueOf(sortedDocs.get(i).getFieldValue("score").toString());
            if (score < config.getScoreThreshold() || score <= cutScoreLowerThan) {
                logger.debug("cut result documents, current score is {}, score threshold is {}", score,
                                config.getScoreThreshold());
                break;
            }
            SolrDocument newDoc = sortedDocs.get(i);
            matches.add(newDoc);
        }
        return matches;
    }

    public BulkHintEvent generateHintsFromSearchResult(final SolrDocumentList docs, final AtomEvent atom,
                    AtomModelWrapper atomModelWrapper, boolean doSuppressHintForAtom,
                    boolean doSuppressHintForMatchedAtoms, boolean kneeDetection) {
        // check if knee detection should be performed
        SolrDocumentList newDocs = docs;
        if (kneeDetection) {
            newDocs = calculateMatchingResults(docs);
        }
        BulkHintEvent bulkHintEvent = new BulkHintEvent();
        logger.info("Received {} matches as query result for atom {}, keeping the top {} ",
                        new Object[] { (docs != null) ? docs.size() : 0, atom, newDocs.size() });
        boolean noHintForMe = atomModelWrapper.flag(WONMATCH.NoHintForMe);
        boolean noHintForCounterpart = atomModelWrapper.flag(WONMATCH.NoHintForCounterpart);
        logger.debug("atom to be matched has NoHintForMe: {}, NoHintForCounterpart: {} ", noHintForMe,
                        noHintForCounterpart);
        for (SolrDocument doc : newDocs) {
            // NOTE: not the whole document is loaded here. The fields that are selected are
            // defined
            // in won.matcher.solr.query.DefaultMatcherQueryExecuter - if additional fields
            // are required, the field list
            // has to be extended in that class.
            String matchedAtomUri = doc.getFieldValue("id").toString();
            if (matchedAtomUri == null) {
                logger.debug("omitting matched atom: could not extract atom URI");
                continue;
            }
            List<String> flags = getValueList(doc, HAS_FLAG_SOLR_FIELD);
            boolean matchedAtomNoHintForMe = flags.contains(WONMATCH.NoHintForMe.toString());
            boolean matchedAtomNoHintForCounterpart = flags.contains(WONMATCH.NoHintForCounterpart.toString());
            // check the matching contexts of the two atoms that are supposed to be matched
            // send only hints to atoms if their matching contexts overlap (if one atom has
            // empty matching context it always receives hints)
            Collection<Object> contextSolrFieldValues = doc
                            .getFieldValues(MatchingContextQueryFactory.MATCHING_CONTEXT_SOLR_FIELD);
            Collection<String> matchedAtomMatchingContexts = new LinkedList<>();
            if (contextSolrFieldValues != null) {
                matchedAtomMatchingContexts = contextSolrFieldValues.stream().map(a -> (String) a)
                                .collect(Collectors.toList());
            }
            Collection<String> matchingContexts = atomModelWrapper.getMatchingContexts();
            if (matchingContexts == null) {
                matchingContexts = new LinkedList<>();
            }
            boolean contextOverlap = CollectionUtils.intersection(matchedAtomMatchingContexts, matchingContexts)
                            .size() > 0;
            boolean suppressHintsForMyContexts = !contextOverlap && !(CollectionUtils.isEmpty(matchingContexts));
            boolean suppressHintsForCounterpartContexts = !contextOverlap
                            && !(CollectionUtils.isEmpty(matchedAtomMatchingContexts));
            // suppress hints for current if its flags or its counterparts flags say so or
            // if it was specified in the calling parameters or matching contexts
            doSuppressHintForAtom = noHintForMe || matchedAtomNoHintForCounterpart || doSuppressHintForAtom
                            || suppressHintsForMyContexts;
            // suppress hints for matched atom if its flags or its counterparts flags say so
            // or if it was specified in the calling parameters or matching contexts
            doSuppressHintForMatchedAtoms = noHintForCounterpart || matchedAtomNoHintForMe
                            || doSuppressHintForMatchedAtoms || suppressHintsForCounterpartContexts;
            if (logger.isDebugEnabled()) {
                logger.debug("matched atom has NoHintForMe: {}, NoHintForCounterpart: {}", matchedAtomNoHintForMe,
                                matchedAtomNoHintForCounterpart);
                logger.debug("atom will receive a hint: {} (uri: {})", !doSuppressHintForAtom, atom.getUri());
                logger.debug("matched atom atom will receive a hint: {} (uri: {})", !doSuppressHintForMatchedAtoms,
                                matchedAtomUri);
                logger.debug("atom matching contexts: {}", matchingContexts);
                logger.debug("matched atom matching contexts: {}", matchedAtomMatchingContexts);
            }
            if (doSuppressHintForAtom && doSuppressHintForMatchedAtoms) {
                logger.debug("no hints to be sent because of Suppress settings");
                continue;
            }
            // wonNodeUri can be returned as either a String or ArrayList, not sure on what
            // this depends
            String wonNodeUri = getFieldValueFirstOfListIfNecessary(doc, WON_NODE_SOLR_FIELD);
            if (wonNodeUri == null) {
                logger.debug("omitting matched atom {}: could not extract WoN node URI", matchedAtomUri);
                continue;
            }
            // normalize the final score
            double score = Double.valueOf(doc.getFieldValue("score").toString()) * config.getScoreNormalizationFactor();
            score = Math.max(0, Math.min(1, score));
            logger.debug("generate hint for match {} with normalized score {}", matchedAtomUri, score);
            if (!doSuppressHintForAtom) {
                bulkHintEvent.addHintEvent(
                                new AtomHintEvent(atom.getUri(), atom.getWonNodeUri(), matchedAtomUri, wonNodeUri,
                                                config.getSolrServerPublicUri(), score, atom.getCause()));
            }
            // also send the same hints to the other side (remote atom and wonnode)?
            if (!doSuppressHintForMatchedAtoms) {
                bulkHintEvent.addHintEvent(
                                new AtomHintEvent(matchedAtomUri, wonNodeUri, atom.getUri(), atom.getWonNodeUri(),
                                                config.getSolrServerPublicUri(), score, atom.getCause()));
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
