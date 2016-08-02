package won.matcher.solr.index;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.service.http.HttpService;
import won.matcher.solr.config.SolrMatcherConfig;
import won.protocol.vocabulary.SFSIG;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by soheilk on 02.09.2015.
 */
@Component
@Scope("prototype")
public class NeedIndexer {

  @Autowired
  private SolrMatcherConfig config;

  @Autowired
  private HttpService httpService;

  public void indexer_jsonld_format(NeedEvent need, Dataset dataset) throws IOException, JsonLdError {

    // TODO: rework this whole indexing method
    LinkedList<String> jsonObjectsList = new LinkedList<String>();

    Iterator<String> names = dataset.listNames();
    while (names.hasNext()) {
        String name = names.next();
        StringWriter sw = new StringWriter();
        Model model = dataset.getNamedModel(name);

        // Add this if you want to exclude one of the graphs, e.g. info graph  => !name.contains("info")&& <=
        if (!model.contains(model.getResource(name), RDF.type, model.getProperty(SFSIG.SIGNATURE.toString()))) {
            RDFDataMgr.write(sw, dataset.getNamedModel(name), Lang.JSONLD);
            String jsonld = sw.toString();
            Object jsonObject = JsonUtils.fromString(jsonld);

            Object frame = JsonUtils.fromString("{\n" + //contextString +
                    "  \"@type\": \"http://purl.org/webofneeds/model#Need\"\n" +
                    "}");
            JsonLdOptions options = new JsonLdOptions();

            Map<String, Object> framed = JsonLdProcessor.frame(jsonObject, frame, options);
            Object graph = framed.get("@graph");

            String prettyFramedGraphString = JsonUtils.toPrettyString(graph);
            jsonObjectsList.add(prettyFramedGraphString);
        }
    }

    String finalJSONFramedNeed = "{\"@graph\":[";
    boolean frameCreated = false;
    for (String jsonObjects : jsonObjectsList) {
      if (jsonObjects.length()>5) {
        if (frameCreated) {
          finalJSONFramedNeed = finalJSONFramedNeed + ",";
        }
        String string = jsonObjects;
        string = string.substring(1);
        string = string.substring(0, string.length() - 1);
        finalJSONFramedNeed = finalJSONFramedNeed + string;
        frameCreated = true;
      }
    }
    finalJSONFramedNeed = finalJSONFramedNeed + "]}";

    // add the uri of the need as id field to avoid multiple adding of needs but instead allow updates
    String idField = need.getUri().toString();
    finalJSONFramedNeed = "{\"id\":\"" + idField + "\"," + finalJSONFramedNeed.substring(1);

    String indexUri = config.getSolrServerUri();
    if (!indexUri.endsWith("/")) {
      indexUri += "/";
    }
    indexUri += "update/json/docs";
    if (config.isCommitIndexedNeedImmediately()) {
      indexUri += "?commit=" + config.isCommitIndexedNeedImmediately();
    }
    httpService.postJsonRequest(indexUri, finalJSONFramedNeed);
  }
}
