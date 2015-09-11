package siren_matcher;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.Dataset;
import common.event.NeedEvent;
import common.service.HttpRequestService;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.response.UpdateResponse;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by soheilk on 02.09.2015.
 */
public class NeedIndexer {


    public void index(NeedObject needObject, SolrServer solrServer) { //TODO it is not working properly

        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("id","heiko1");//This is mandatory
        solrInputDocument.addField("@graph.@id", "teeeeee3456e4eeeeeest12");
        //solrInputDocument.addField("@graph.@id", needObject.getWonUri());
        solrInputDocument.addField("@graph.http://purl.org/webofneeds/model#hasContent.http://purl.org/dc/elements/1.1/title", needObject.getNeedTitle());
        // solrInputDocument.addField("@graph.http://purl.org/webofneeds/model#hasContent.http://purl.org/dc/elements/1.1/title", "teeest");
        //solrInputDocument.addField("@graph.http://purl.org/webofneeds/model#hasBasicNeedType.@id", needObject.getBasicNeedType());
        solrInputDocument.addField("@graph.http://purl.org/webofneeds/model#hasBasicNeedType.@id", "http://purl.org/webofneeds/model#demand");

        solrInputDocument.addField("@graph.http://purl.org/webofneeds/model#hasContent.http://purl.org/webofneeds/model#hasTextDescription", needObject.getNeedDescription());


/*        UpdateRequest req = new UpdateRequest();
        req.setAction(UpdateRequest.ACTION.COMMIT, false, false);
        req.add(solrInputDocument );
        try {
            UpdateResponse rsp = req.process( solrServer );
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace(); //TODO
        }

        try {
            solrServer.commit();
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace(); //TODO
        }*/


       try {
            solrServer.add(solrInputDocument);
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();//TODO
        }
        try {
            solrServer.commit();
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();//TODO
        }




    }


    public void indexer_jsonld_format(Dataset dataset){

               try {
            //convert to json-ld for indexing
            StringWriter sw = new StringWriter(); //TODO: delete
            RDFDataMgr.write(sw, dataset, Lang.JSONLD); //TODO: delete
            RDFDataMgr.write(System.out, dataset, Lang.JSONLD);
            String jsonld = sw.toString(); //TODO: delete
            Object jsonObject = null; //TODO: delete
            jsonObject = JsonUtils.fromString(jsonld); //TODO: delete
            JsonLdOptions options = new JsonLdOptions(); //TODO: delete
            options.setEmbed(true);
            Map context = new HashMap(); //TODO: delete
            //Object compact = JsonLdProcessor.expand(jsonObject, options); //TODO: delete
            String frameString = "{\n" +
                    "  \"@context\": {\n" +
                    "    \"won\": \"http://purl.org/webofneeds/model#\"\n" +
                    "  },\n" +
                    "  \"@type\": \"won:Need\"\n" +
                    "}";
            System.out.println("frame:" +frameString);
            Object frame = JsonUtils.fromReader(new StringReader(frameString));
            Object compact = JsonLdProcessor.frame(jsonObject, frame, options); //TODO: delete
            System.out.println("jsonld, compacted: " + JsonUtils.toPrettyString(compact)); //TODO: delete


             //JUST FOR TEEEEEEEST!
             String jsonCopiedFromYimOutput ="{\n" +
                     "\t\"@graph\": [{\n" +
                     "\t\t\"http://purl.org/dc/terms/created\": {\n" +
                     "\t\t\t\"@value\": \"2015-03-26T11:56:59.44Z\",\n" +
                     "\t\t\t\"@type\": \"http://www.w3.org/2001/XMLSchema#dateTime\"\n" +
                     "\t\t},\n" +
                     "\t\t\"http://purl.org/webofneeds/model#hasBasicNeedType\": {\n" +
                     "\t\t\t\"@id\": \"http://purl.org/webofneeds/model#Supply\"\n" +
                     "\t\t},\n" +
                     "\t\t\"http://purl.org/webofneeds/model#hasEventContainer\": {\n" +
                     "\t\t\t\"http://www.w3.org/2000/01/rdf-schema#member\": {\n" +
                     "\t\t\t\t\"@id\": \"http://localhost:8080/won/resource/event/nfy2g98yrokbbid8jntz\"\n" +
                     "\t\t\t},\n" +
                     "\t\t\t\"@type\": \"http://purl.org/webofneeds/model#EventContainer\",\n" +
                     "\t\t\t\"@id\": \"http://localhost:8080/won/resource/need/ob6qfzt9zjmx1semf7es/events\"\n" +
                     "\t\t},\n" +
                     "\t\t\"http://purl.org/webofneeds/model#hasFacet\": {\n" +
                     "\t\t\t\"@id\": \"http://purl.org/webofneeds/model#OwnerFacet\"\n" +
                     "\t\t},\n" +
                     "\t\t\"http://purl.org/webofneeds/model#hasContent\": {\n" +
                     "\t\t\t\"http://purl.org/dc/elements/1.1/title\": \"[FCNYC] OFFER:  blank audio cassettes, cases, inserts (UWS)\",\n" +
                     "\t\t\t\"@type\": \"http://purl.org/webofneeds/model#NeedContent\",\n" +
                     "\t\t\t\"http://purl.org/webofneeds/model#hasTextDescription\": \"\\r\\n> I have a bunch of blank audio cassette tapes, empty cassette boxes and\\r\\n> cassette inserts and labels.  Maybe 30 or more boxes, about 20 blank\\r\\n> tapes.  Lots of inserts.  I hope someone use these?\\r\\n>\\r\\n> Please email me with possible times in the next few days that you\\r\\n> could stop by to pick up.  I prefer to deal with email rather than  \\r\\n> texting.\\r\\n\\r\\n>   Pickup on Upper    West Side, high 80's on Broadway\\r\\n\\r\\n> Thank you.\\r\\n>\\r\\n> Robin\\r\\n> windyrec@aol.com\\r\\n\\r\\n\\r\\n\",\n" +
                     "\t\t\t\"@id\": \"_:b0\"\n" +
                     "\t\t},\n" +
                     "\t\t\"@type\": \"http://purl.org/webofneeds/model#Need\",\n" +
                     "\t\t\"http://purl.org/webofneeds/model#hasNeedModality\": {\n" +
                     "\t\t\t\"@type\": \"http://purl.org/webofneeds/model#NeedModality\",\n" +
                     "\t\t\t\"@id\": \"_:b1\"\n" +
                     "\t\t},\n" +
                     "\t\t\"http://purl.org/webofneeds/model#hasConnections\": {\n" +
                     "\t\t\t\"@id\": \"http://localhost:8080/won/resource/need/ob6qfzt9zjmx1semf7es/connections/\"\n" +
                     "\t\t},\n" +
                     "\t\t\"http://purl.org/webofneeds/model#hasWonNode\": {\n" +
                     "\t\t\t\"@id\": \"http://localhost:8080/won/resource\"\n" +
                     "\t\t},\n" +
                     "\t\t\"http://purl.org/webofneeds/model#isInState\": {\n" +
                     "\t\t\t\"@id\": \"http://purl.org/webofneeds/model#Active\"\n" +
                     "\t\t},\n" +
                     "\t\t\"@id\": \"http://localhost:8080/won/resource/need/ob6qfzt9zjmx1semf7es\"\n" +
                     "\t}]\n" +
                     "}";




           HttpRequestService httpService = new HttpRequestService();
           httpService.requestSirenTest("http://localhost:8983/solr/WoN/siren/add", jsonCopiedFromYimOutput);
        } catch (IOException e) { //TODO: delete
            e.printStackTrace(); //TODO: delete
        } catch (JsonLdError jsonLdError) { //TODO: delete
            jsonLdError.printStackTrace(); //TODO: delete
        }



    }
}
