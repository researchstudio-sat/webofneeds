package siren_matcher;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDF;
import common.service.HttpRequestService;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import won.protocol.vocabulary.SFSIG;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by soheilk on 02.09.2015.
 */
public class NeedIndexer {


    public void indexer_jsonld_format(Dataset dataset) {

        try {
            ArrayList<String> jsonObjectsList = new ArrayList<String>();


            Iterator<String> names = dataset.listNames();
            while (names.hasNext()) {
                String name = names.next();
                StringWriter sw = new StringWriter();
                Model model = dataset.getNamedModel(name);

                // Add this if you want to exclude one of the graphs, e.g. info graph  => !name.contains("info")&& <=
                if (!model.contains(model.getResource(name), RDF.type, model.getProperty(SFSIG.SIGNATURE.toString()))) {
                    // System.out.println("NEXT String" + name); //For testing
                    // RDFDataMgr.write(System.out, dataset, Lang.TRIG); //For testing
                    RDFDataMgr.write(sw, dataset.getNamedModel(name), Lang.JSONLD);
                    //RDFDataMgr.write(System.out, dataset.getNamedModel(name), Lang.JSONLD); //For testing
                    String jsonld = sw.toString();
                    // System.out.println("jsonld is "+jsonld); //For testing
                    Object jsonObject = JsonUtils.fromString(jsonld);

                    //System.out.println("jsonObject: "+ jsonObject); //For testing
                    //System.out.println("jsonObject ENDSSSSSSSSS: "); //For testing

                    // We don't use the context in framing but let's keep this for potential future usages
                           /*String contextString =  "  \"@context\": {\n" +
                                   "    \"hasGraph\": {\n" +
                                   "      \"@id\": \"http://purl.org/webofneeds/model#hasGraph\",\n" +
                                   "      \"@type\": \"@id\"\n" +
                                   "    },\n" +
                                   "    \"msg\": \"http://purl.org/webofneeds/message#\",\n" +
                                   "    \"conn\": \"http://localhost:8080/won/resource/won/resource/connection/\",\n" +
                                   "    \"woncrypt\": \"http://purl.org/webofneeds/woncrypt#\",\n" +
                                   "    \"need\": \"http://localhost:8080/won/resource/need/\",\n" +
                                   "    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\",\n" +
                                   "    \"cert\": \"http://www.w3.org/ns/auth/cert#\",\n" +
                                   "    \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n" +
                                   "    \"local\": \"http://localhost:8080/won/resource/\",\n" +
                                   "    \"geo\": \"http://www.w3.org/2003/01/geo/wgs84_pos#\",\n" +
                                   "    \"rdf\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\n" +
                                   "    \"won\": \"http://purl.org/webofneeds/model#\",\n" +
                                   "    \"ldp\": \"http://www.w3.org/ns/ldp#\",\n" +
                                   "    \"event\": \"http://localhost:8080/won/resource/event/\",\n" +
                                   "    \"sioc\": \"http://rdfs.org/sioc/ns#\",\n" +
                                   "    \"dc\": \"http://purl.org/dc/elements/1.1/\"\n" +
                                   "  },\n";*/

                    Object frame = JsonUtils.fromString("{\n" + //contextString +
                            "  \"@type\": \"http://purl.org/webofneeds/model#Need\"\n" +
                            "}");
                    JsonLdOptions options = new JsonLdOptions();

                    Map<String, Object> framed = JsonLdProcessor.frame(jsonObject, frame, options);
                    Object graph = framed.get("@graph");

                    // System.out.println("@framed>> "+ framed); //For testing
                    // System.out.println("ends "); //For testing

                    String prettyFramedGraphString = JsonUtils.toPrettyString(graph);
                    // System.out.println("framed: "+ prettyFramedGraphString); //For testing

                    // System.out.println("framed: ENDS<<<<<<<<<<<<<<<<<<<<<<<<"); //For testing

                    jsonObjectsList.add(prettyFramedGraphString);

                }
            }


            String finalJSONFramedNeed = "{\"@graph\":[";
            Boolean EscapeFlag = false;
            for (int i = 0; i < jsonObjectsList.size(); i++) {
              if (jsonObjectsList.get(i).length()>5) {
                if (i != 0 && EscapeFlag==false) {
                  finalJSONFramedNeed = finalJSONFramedNeed + ",";
                }
                EscapeFlag = false;
                String string = jsonObjectsList.get(i);
                string = string.substring(1);
                string = string.substring(0, string.length() - 1);
                finalJSONFramedNeed = finalJSONFramedNeed + string;
              }
              else{
                EscapeFlag = true;
              }
            }
            finalJSONFramedNeed = finalJSONFramedNeed + "]}";

             System.out.println("Final JSON Framed NEED "+finalJSONFramedNeed); //For testing
            HttpRequestService httpService = new HttpRequestService();
            // String jsonData = sw.toString();
            httpService.postRequest(Configuration.sIREnUri+"siren/add?commit=true", finalJSONFramedNeed);


            // HttpRequestService httpService = new HttpRequestService();
            // httpService.requestSirenTest("http://localhost:8983/solr/WoN/siren/add", jsonCopiedFromYimOutput);
        } catch (IOException e) {
            e.printStackTrace(); //TODO:
        } catch (JsonLdError jsonLdError) {
            jsonLdError.printStackTrace(); //TODO:
        }

    }
}
