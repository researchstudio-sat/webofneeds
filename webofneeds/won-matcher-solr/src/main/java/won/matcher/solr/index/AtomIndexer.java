package won.matcher.solr.index;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
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

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import won.matcher.service.common.service.http.HttpService;
import won.matcher.solr.config.SolrMatcherConfig;
import won.protocol.model.Coordinate;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.AtomModelWrapper;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMATCH;

/**
 * Created by hfriedrich on 03.08.2016.
 */
@Component
@Scope("prototype")
public class AtomIndexer {
    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String SOLR_IS_LOCATION_COORDINATES_FIELD = "is_atom_location";
    public static final String SOLR_SEEKS_LOCATION_COORDINATES_FIELD = "seeks_atom_location";
    public static final String SOLR_SEEKS_SEEKS_LOCATION_COORDINATES_FIELD = "seeksSeeks_atom_location";
    // SPARQL query to contruct an atom object out of the dataset, use all graphs
    // that reference "won:Atom"
    private static final String ATOM_INDEX_QUERY = "prefix won: <https://w3id.org/won/core#> construct { ?a ?b ?c .} where { "
                    + "GRAPH ?graph { ?atom a won:Atom. ?a ?b ?c. } }";
    @Autowired
    private SolrMatcherConfig config;
    @Autowired
    private HttpService httpService;

    public void index(Dataset dataset) throws IOException, JsonLdError {
        // serialize the atom Dataset to jsonld
        Query query = QueryFactory.create(ATOM_INDEX_QUERY);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            Model atomModel = qexec.execConstruct();
            // normalize the atom model for solr indexing
            AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomModel, null);
            String atomUri = atomModelWrapper.getAtomUri();
            atomModel = atomModelWrapper.normalizeAtomModel();
            // check if test index should be used for atom
            boolean usedForTesting = atomModelWrapper.flag(WONMATCH.UsedForTesting);
            indexAtomModel(atomModel, atomUri, usedForTesting);
        }
    }

    public void indexAtomModel(Model atomModel, String id, boolean useTestCore) throws IOException, JsonLdError {
        // create the json from rdf model
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, atomModel, Lang.JSONLD);
        String jsonld = sw.toString();
        Object jsonObject = JsonUtils.fromString(jsonld);
        Object frame = JsonUtils.fromString(" {\"@type\": \"" + WON.Atom + "\"} ");
        JsonLdOptions options = new JsonLdOptions();
        Map<String, Object> framed = JsonLdProcessor.frame(jsonObject, frame, options);
        // add the uri of the atom as id field to avoid multiple adding of atoms but
        // instead allow updates
        framed.put("id", id);
        // add latitude and longitude values in one field for Solr spatial queries
        DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomModel, null);
        Resource atomContentNode = atomModelWrapper.getAtomContentNode();
        Coordinate atomCoordinate = atomModelWrapper.getLocationCoordinate(atomContentNode);
        if (atomCoordinate != null) {
            framed.put(SOLR_IS_LOCATION_COORDINATES_FIELD, String.valueOf(atomCoordinate.getLatitude()) + ","
                            + String.valueOf(atomCoordinate.getLongitude()));
        }
        for (Resource contentNode : atomModelWrapper.getSeeksNodes()) {
            Coordinate coordinate = atomModelWrapper.getLocationCoordinate(contentNode);
            if (coordinate != null) {
                framed.put(SOLR_SEEKS_LOCATION_COORDINATES_FIELD, String.valueOf(coordinate.getLatitude()) + ","
                                + String.valueOf(coordinate.getLongitude()));
            }
        }
        for (Resource contentNode : atomModelWrapper.getSeeksSeeksNodes()) {
            Coordinate coordinate = atomModelWrapper.getLocationCoordinate(contentNode);
            if (coordinate != null) {
                framed.put(SOLR_SEEKS_SEEKS_LOCATION_COORDINATES_FIELD, String.valueOf(coordinate.getLatitude()) + ","
                                + String.valueOf(coordinate.getLongitude()));
            }
        }
        // write the final json string
        sw = new StringWriter();
        JsonUtils.writePrettyPrint(sw, framed);
        String atomJson = sw.toString();
        // post the atom to the solr index
        String indexUri = config.getSolrEndpointUri(useTestCore);
        indexUri += "update/json/docs";
        if (config.isCommitIndexedAtomImmediately()) {
            indexUri += "?commit=" + config.isCommitIndexedAtomImmediately();
        }
        log.debug("Post atom to solr index. \n Solr URI: {} \n Atom (JSON): {}", indexUri, atomJson);
        try {
            httpService.postJsonRequest(indexUri, atomJson);
        } catch (HttpClientErrorException e) {
            log.info("Error indexing atom with solr. \n Solr URI: {} \n Atom (JSON): {}", indexUri, atomJson);
        }
    }
}
