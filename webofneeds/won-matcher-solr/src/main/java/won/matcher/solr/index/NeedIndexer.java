package won.matcher.solr.index;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import won.matcher.service.common.service.http.HttpService;
import won.matcher.solr.config.SolrMatcherConfig;
import won.protocol.model.Coordinate;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.NeedModelWrapper;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Created by hfriedrich on 03.08.2016.
 */
@Component
@Scope("prototype")
public class NeedIndexer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String SOLR_IS_LOCATION_COORDINATES_FIELD = "is_need_location";
    public static final String SOLR_SEEKS_LOCATION_COORDINATES_FIELD = "seeks_need_location";
    public static final String SOLR_SEEKS_SEEKS_LOCATION_COORDINATES_FIELD = "seeksSeeks_need_location";

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
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            Model needModel = qexec.execConstruct();
    
            // normalize the need model for solr indexing
            NeedModelWrapper needModelWrapper = new NeedModelWrapper(needModel, null);
            String needUri = needModelWrapper.getNeedUri();
            needModel = needModelWrapper.normalizeNeedModel();
    
            // check if test index should be used for need
            boolean usedForTesting = needModelWrapper.hasFlag(WON.USED_FOR_TESTING);
            indexNeedModel(needModel, needUri, usedForTesting);
        }
    }

    public void indexNeedModel(Model needModel, String id, boolean useTestCore) throws IOException, JsonLdError {

        // create the json from rdf model
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, needModel, Lang.JSONLD);
        String jsonld = sw.toString();
        Object jsonObject = JsonUtils.fromString(jsonld);
        Object frame = JsonUtils.fromString(" {\"@type\": \"" + WON.NEED + "\"} ");
        JsonLdOptions options = new JsonLdOptions();
        Map<String, Object> framed = JsonLdProcessor.frame(jsonObject, frame, options);

        // add the uri of the need as id field to avoid multiple adding of needs but instead allow updates
        framed.put("id", id);

        // add latitude and longitude values in one field for Solr spatial queries
        DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needModel, null);

        Resource needContentNode = needModelWrapper.getNeedContentNode();
        Coordinate needCoordinate = needModelWrapper.getLocationCoordinate(needContentNode);
        if (needCoordinate != null) {
            framed.put(SOLR_IS_LOCATION_COORDINATES_FIELD, String.valueOf(needCoordinate.getLatitude()) + "," + String.valueOf(needCoordinate.getLongitude()));
        }

        for (Resource contentNode : needModelWrapper.getSeeksNodes()) {
            Coordinate coordinate = needModelWrapper.getLocationCoordinate(contentNode);
            if (coordinate != null) {
                framed.put(SOLR_SEEKS_LOCATION_COORDINATES_FIELD, String.valueOf(coordinate.getLatitude()) + "," + String.valueOf(coordinate.getLongitude()));
            }
        }

        for (Resource contentNode : needModelWrapper.getSeeksSeeksNodes()) {
            Coordinate coordinate = needModelWrapper.getLocationCoordinate(contentNode);
            if (coordinate != null) {
                framed.put(SOLR_SEEKS_SEEKS_LOCATION_COORDINATES_FIELD, String.valueOf(coordinate.getLatitude()) + "," + String.valueOf(coordinate.getLongitude()));
            }
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
        log.debug("Post need to solr index. \n Solr URI: {} \n Need (JSON): {}", indexUri, needJson);
        try {
          httpService.postJsonRequest(indexUri, needJson);
        } catch (HttpClientErrorException e) {
          log.info("Error indexing need with solr. \n Solr URI: {} \n Need (JSON): {}", indexUri, needJson);
        }
    }
}
