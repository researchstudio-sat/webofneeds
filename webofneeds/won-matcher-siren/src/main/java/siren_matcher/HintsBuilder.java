package siren_matcher;

import common.event.HintEvent;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.ArrayList;

import static jdk.nashorn.internal.objects.NativeNumber.valueOf;

/**
 * Created by soheilk on 04.09.2015.
 */
public class HintsBuilder {
   public ArrayList<HintEvent> produceFinalNormalizeHints(ArrayList<SolrDocumentList> solrDocListArrayList, String targetNeedUri) {
       ArrayList<SolrDocumentList> normalizedSolrDocListArrayList = new ArrayList<SolrDocumentList>();
       for(int i=0; i<solrDocListArrayList.size();i++) {
           normalizedSolrDocListArrayList.add(normalizer(solrDocListArrayList.get(i)));
       }

       SolrDocumentList aggregatedSolrDocumentList = new SolrDocumentList();

       for(int j=0; j<normalizedSolrDocListArrayList.get(0).getNumFound() && j< Configuration.NUMBER_OF_HINTS; j++) {
           aggregatedSolrDocumentList.add(j, normalizedSolrDocListArrayList.get(0).get(j)); //put the first query results in the aggregatedSolrDocumentList
       }

       for(int k=1; k<normalizedSolrDocListArrayList.size();k++){ //now we consider other queries results; k starts from 1
           SolrDocumentList currentSolrDocumentList = normalizedSolrDocListArrayList.get(k);
           for(int l=0; l<currentSolrDocumentList.getNumFound() && l< Configuration.NUMBER_OF_HINTS;l++){
               boolean foundInTheAggregatedList = false;
               SolrDocument currentDocuentInTheCurrentList = currentSolrDocumentList.get(l);
               for(int m=0; m < aggregatedSolrDocumentList.size();m++){
                   SolrDocument currentDocumentInTheAggregatedSolrDocumentList = aggregatedSolrDocumentList.get(m);
                   if (currentDocumentInTheAggregatedSolrDocumentList.getFieldValue("@graph.@id").equals(currentDocuentInTheCurrentList.getFieldValue("@graph.@id"))){
                       aggregatedSolrDocumentList.get(m).setField("score",(float)currentDocumentInTheAggregatedSolrDocumentList.getFieldValue("score")+(float)currentDocuentInTheCurrentList.getFieldValue("score"));
                       foundInTheAggregatedList = true;
                       break;
                   }
               }
               if (foundInTheAggregatedList == false) { // does not exist in the aggregated list
                   aggregatedSolrDocumentList.add(currentSolrDocumentList.get(l));
               }
           }
       }

       for (int n=0; n<aggregatedSolrDocumentList.size();n++) {
           aggregatedSolrDocumentList.get(n).setField("score",(float)aggregatedSolrDocumentList.get(n).getFieldValue("score")/normalizedSolrDocListArrayList.size());
       }

       ArrayList<HintEvent> hintEventLists = new ArrayList<HintEvent>();

       for (int o=0; o<aggregatedSolrDocumentList.size();o++) {
           hintEventLists.add(new HintEvent(targetNeedUri, aggregatedSolrDocumentList.get(o).getFieldValue("@graph.@id").toString(), valueOf(aggregatedSolrDocumentList.get(o).getFieldValue("score"))));
       }

       return hintEventLists;
   }
   private SolrDocumentList normalizer(SolrDocumentList solrDocList){
       float maxScore = solrDocList.getMaxScore();
       for(int i=0;i<solrDocList.getNumFound() && i<Configuration.NUMBER_OF_HINTS;i++) {
           solrDocList.get(i).setField("score",(float)solrDocList.get(i).getFieldValue("score")/maxScore);
       }
       return solrDocList;
   }
}
