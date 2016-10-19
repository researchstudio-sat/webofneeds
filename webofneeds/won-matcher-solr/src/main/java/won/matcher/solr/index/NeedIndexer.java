package won.matcher.solr.index;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import won.matcher.service.common.service.http.HttpService;
import won.matcher.solr.config.SolrMatcherConfig;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;

/**
 * Created by hfriedrich on 03.08.2016.
 */
@Component
@Scope("prototype")
public class NeedIndexer {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final String SOLR_LOCATION_COORDINATES_FIELD = "need_location";

  // SPARQL query to contruct a need object out of the dataset, use all graphs that reference "won:Need"
  private static final String NEED_INDEX_QUERY =
    "prefix won: <http://purl.org/webofneeds/model#> construct { ?a ?b ?c .} where { " +
      "GRAPH ?graph { ?need a won:Need. ?a ?b ?c. } }";

  @Autowired
  private SolrMatcherConfig config;

  @Autowired
  private HttpService httpService;

  public void index(Dataset dataset) throws IOException, JsonLdError {

    // serialize the need Dataset to jsonld
    Query query = QueryFactory.create(NEED_INDEX_QUERY);
    QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
    Model needModel = qexec.execConstruct();
    String needUri = WonRdfUtils.NeedUtils.getNeedURI(needModel).toString();

    // check if test index should be used for need
    boolean usedForTesting = WonRdfUtils.NeedUtils.hasFlag(dataset, needUri, WON.USED_FOR_TESTING);
    indexNeedModel(needModel, needUri, usedForTesting);
  }

  public void indexNeedModel(Model needModel, String id, boolean useTestCore) throws IOException, JsonLdError {

    // create the json from rdf model
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, needModel, Lang.JSONLD);
    String jsonld = sw.toString();
    Object jsonObject = JsonUtils.fromString(jsonld);
    Object frame = JsonUtils.fromString(" {\"@type\": \""+ WON.NEED + "\"} ");
    JsonLdOptions options = new JsonLdOptions();
    Map<String, Object> framed = JsonLdProcessor.frame(jsonObject, frame, options);

    // add the uri of the need as id field to avoid multiple adding of needs but instead allow updates
    framed.put("id", id);

    // add latitude and longitude values in one field for Solr spatial queries
    URI needUri = WonRdfUtils.NeedUtils.getNeedURI(needModel);
    Float longitude = WonRdfUtils.NeedUtils.getLocationLongitude(needModel, needUri);
    Float latitude = WonRdfUtils.NeedUtils.getLocationLatitude(needModel, needUri);
    if (latitude != null && longitude != null) {
      framed.put(SOLR_LOCATION_COORDINATES_FIELD, latitude.toString() + "," + longitude.toString());
    }

    // write the final json string
    sw = new StringWriter();
    JsonUtils.writePrettyPrint(sw, framed);
    String needJson = sw.toString();

    // post the need to the solr index
    String indexUri = config.getSolrEndpointUri(useTestCore);
    indexUri += "update/json/docs";
    if (config.isCommitIndexedNeedImmediately()) {
      indexUri += "?commit=" + config.isCommitIndexedNeedImmediately();
    }
    log.debug("Post needto solr index. \n Solr URI: {} \n Need (JSON): {}", indexUri, needJson);
    httpService.postJsonRequest(indexUri, needJson);
  }
}
