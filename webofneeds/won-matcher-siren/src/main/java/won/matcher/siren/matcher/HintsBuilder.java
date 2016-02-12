package won.matcher.siren.matcher;

import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.siren.config.SirenMatcherConfig;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by soheilk on 04.09.2015.
 */
@Component
public class HintsBuilder {

    @Autowired
    private SirenMatcherConfig config;

    public BulkHintEvent produceFinalNormalizeHints(ArrayList<SolrDocumentList> solrDocListArrayList, String targetNeedUri, String targetWonNode) {

        ArrayList<Map<String, SolrDocument>> normalizedSolrDocListArrayList = new ArrayList<Map<String, SolrDocument>>();
        for (int i = 0; i < solrDocListArrayList.size(); i++) {
            normalizedSolrDocListArrayList.add(basicNormalizer(solrDocListArrayList.get(i), solrDocListArrayList.size()));
        }

        Map<String, SolrDocument> aggregatedSolrDocumentsMap = normalizedSolrDocListArrayList.get(0); //put the results of the first query in the aggregatedSolrDocumentLis
        for(int i=1 ; i<normalizedSolrDocListArrayList.size();i++) { //starts from the second one!
            Map<String, SolrDocument> currenSolrDocumentsMap = normalizedSolrDocListArrayList.get(i);
            //Iterate over each document in the map
            for (Map.Entry<String, SolrDocument> entry : currenSolrDocumentsMap.entrySet())
            {
                if(aggregatedSolrDocumentsMap.containsKey(entry.getKey())){
                    ((SolrDocument) aggregatedSolrDocumentsMap.get(entry.getKey())).setField("score",
                            (float)((SolrDocument) aggregatedSolrDocumentsMap.get(entry.getKey())).getFieldValue("score")+
                                    (float)entry.getValue().getFieldValue("score"));
                }
                else {
                    aggregatedSolrDocumentsMap.put(entry.getKey(), entry.getValue());
                }
            }

        }

        //Inorder to be able to cut the result with based on a threshold, we need to sort the results based on the scores.
        SolrDocumentList aggregatedSolrDocumentList = new SolrDocumentList();

        for (Map.Entry<String, SolrDocument> entry : aggregatedSolrDocumentsMap.entrySet())
        {
            aggregatedSolrDocumentList.add(entry.getValue());
        }

        aggregatedSolrDocumentList.sort(new Comparator<SolrDocument>() {
            @Override
            public int compare(SolrDocument o1, SolrDocument o2) {
                if((float)o1.getFieldValue("score")>(float)o2.getFieldValue("score"))
                return -1;
                else if((float)o1.getFieldValue("score")<(float)o2.getFieldValue("score"))
                    return 1;
                else
                    return 0;
            }
        });

        BulkHintEvent bulkHintEvent = new BulkHintEvent();


        for (int i = 0; i < aggregatedSolrDocumentList.size() && i < config.getMaxHints(); i++) {
            String needUri = ((List)aggregatedSolrDocumentList.get(i).getFieldValue("@graph.@id")).get(0).toString();
            String wonNodeUri = ((List)aggregatedSolrDocumentList.get(i).getFieldValue("@graph.http://purl.org/webofneeds/model#hasWonNode.@id")).get(0).toString();
            double score = Double.valueOf(aggregatedSolrDocumentList.get(i).getFieldValue("score").toString());
            if (score < config.getScoreThreshold()) {
                break;
            }
            // since we cannot query the wonNodeUri of the document in solr at the same time as the needUri, we just
            // set the needUri in the hint event and let the matching service figure out to which won node it belongs
            bulkHintEvent.addHintEvent(new HintEvent(targetWonNode, targetNeedUri, wonNodeUri, needUri,
                                                     config.getSolrServerPublicUri(), score));
        }

        return bulkHintEvent;
    }

    /*
    This is a basic normalizer that sets the score of the most similar document to 1 and calculates scores of the other
    documents based on it; it also weights the score based on the numberOfPerformedQueries
    */
    private Map<String, SolrDocument> basicNormalizer(SolrDocumentList solrDocList, int numberOfPerformedQueries) {
        Map<String, SolrDocument> solrDocsMap = new HashMap<String, SolrDocument>();
        float maxScore = solrDocList.getMaxScore();
        for (int i = 0; i < solrDocList.size() && i < solrDocList.getNumFound() && i < config.getMaxHints(); i++) {
            solrDocList.get(i).setField("score", ((float) solrDocList.get(i).getFieldValue("score") / maxScore) / numberOfPerformedQueries);
            SolrDocument currentSolrDocument = solrDocList.get(i);
            solrDocsMap.put(currentSolrDocument.getFieldValue("@graph.@id").toString(), currentSolrDocument);
        }
        return solrDocsMap;
    }
}
