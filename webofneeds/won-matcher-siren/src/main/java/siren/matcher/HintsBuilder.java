package siren.matcher;

import common.event.HintEvent;
import config.SirenMatcherConfig;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by soheilk on 04.09.2015.
 */
@Component
public class HintsBuilder {

    @Autowired
    private SirenMatcherConfig config;

    public ArrayList<HintEvent> produceFinalNormalizeHints(ArrayList<SolrDocumentList> solrDocListArrayList, String targetNeedUri, String targetWonNode) {
        ArrayList<SolrDocumentList> normalizedSolrDocListArrayList = new ArrayList<SolrDocumentList>();
        for (int i = 0; i < solrDocListArrayList.size(); i++) {
            normalizedSolrDocListArrayList.add(normalizer(solrDocListArrayList.get(i)));
        }

        SolrDocumentList aggregatedSolrDocumentList = new SolrDocumentList();

        for (int j = 0; j < normalizedSolrDocListArrayList.get(0).getNumFound() && j < config.getMaxHints(); j++) {
            aggregatedSolrDocumentList.add(j, normalizedSolrDocListArrayList.get(0).get(j)); //put the first query results in the aggregatedSolrDocumentList
        }

        for (int k = 1; k < normalizedSolrDocListArrayList.size(); k++) { //now we consider other queries results; k starts from 1
            SolrDocumentList currentSolrDocumentList = normalizedSolrDocListArrayList.get(k);
            for (int l = 0; l < currentSolrDocumentList.getNumFound() && l < config.getMaxHints(); l++) {
                boolean foundInTheAggregatedList = false;
                SolrDocument currentDocuentInTheCurrentList = currentSolrDocumentList.get(l);
                for (int m = 0; m < aggregatedSolrDocumentList.size(); m++) {
                    SolrDocument currentDocumentInTheAggregatedSolrDocumentList = aggregatedSolrDocumentList.get(m);
                    if (currentDocumentInTheAggregatedSolrDocumentList.getFieldValue("@graph.@id").equals(currentDocuentInTheCurrentList.getFieldValue("@graph.@id"))) {
                        aggregatedSolrDocumentList.get(m).setField("score", (float) currentDocumentInTheAggregatedSolrDocumentList.getFieldValue("score") + (float) currentDocuentInTheCurrentList.getFieldValue("score"));
                        foundInTheAggregatedList = true;
                        break;
                    }
                }
                if (foundInTheAggregatedList == false) { // does not exist in the aggregated list
                    aggregatedSolrDocumentList.add(currentSolrDocumentList.get(l));
                }
            }
        }

        for (int n = 0; n < aggregatedSolrDocumentList.size(); n++) {
            aggregatedSolrDocumentList.get(n).setField("score", (float) aggregatedSolrDocumentList.get(n).getFieldValue("score") / normalizedSolrDocListArrayList.size());
        }

        ArrayList<HintEvent> hintEventLists = new ArrayList<HintEvent>();
        for (int o = 0; o < aggregatedSolrDocumentList.size(); o++) {
            String needUri = ((List)aggregatedSolrDocumentList.get(o).getFieldValue("@graph.@id")).get(0).toString();
            String wonNodeUri = ((List)aggregatedSolrDocumentList.get(o).getFieldValue("@graph.http://purl.org/webofneeds/model#hasWonNode.@id")).get(0).toString();
            double score = Double.valueOf(aggregatedSolrDocumentList.get(o).getFieldValue("score").toString());

            // since we cannot query the wonNodeUri of the document in solr at the same time as the needUri, we just
            // set the needUri in the hint event and let the matching service figure out to which won node it belongs
            hintEventLists.add(new HintEvent(targetWonNode, targetNeedUri, wonNodeUri, needUri, config.getSolrServerUri(),
                                             score));
        }

        return hintEventLists;
    }

    private SolrDocumentList normalizer(SolrDocumentList solrDocList) {
        float maxScore = solrDocList.getMaxScore();
        for (int i = 0; i < solrDocList.getNumFound() && i < config.getMaxHints(); i++) {
            solrDocList.get(i).setField("score", (float) solrDocList.get(i).getFieldValue("score") / maxScore);
        }
        return solrDocList;
    }
}
