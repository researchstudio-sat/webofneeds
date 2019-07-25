package won.matcher.service.nodemanager.service;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import won.matcher.service.common.service.sparql.SparqlService;
import won.protocol.exception.DataIntegrityException;
import won.protocol.service.WonNodeInfo;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

/**
 * Sparql service extended with methods for won node controller User: hfriedrich
 * Date: 04.05.2015
 */
@Component
public class WonNodeSparqlService extends SparqlService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public WonNodeSparqlService(@Value("${uri.sparql.endpoint}") final String sparqlEndpoint) {
        super(sparqlEndpoint);
    }

    /**
     * Retrieve resource data of all known won nodes that are saved in the Sparql
     * endpoint.
     *
     * @return Set of all known won node resource data
     */
    public Set<WonNodeInfo> retrieveAllWonNodeInfo() {
        Set<WonNodeInfo> wonNodeInfos = new HashSet<>();
        String queryString = "SELECT ?graphUri ?nodeUri WHERE { GRAPH ?graphUri {?nodeUri won:uriPrefixSpecification ?c} }";
        ParameterizedSparqlString pps = new ParameterizedSparqlString();
        pps.setCommandText(queryString);
        pps.setNsPrefix("won", "https://w3id.org/won/core#");
        logger.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
        logger.debug("Execute query: {}", pps.toString());
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, pps.asQuery())) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution qs = results.nextSolution();
                RDFNode rdfNode = qs.get("graphUri");
                if (rdfNode != null) {
                    String graphUri = rdfNode.asResource().getURI();
                    Dataset ds = retrieveDataset(graphUri);
                    WonNodeInfo nodeInfo = getWonNodeInfoFromDataset(ds);
                    wonNodeInfos.add(nodeInfo);
                }
            }
            return wonNodeInfos;
        }
    }

    /**
     * Get the {@link won.protocol.service.WonNodeInfo} as an object from a
     * {@link Dataset}
     *
     * @param ds Dataset which holds won node information
     * @return
     */
    public WonNodeInfo getWonNodeInfoFromDataset(Dataset ds) {
        String dsWonNodeUri = getWonNodeUriFromDataset(ds);
        WonNodeInfo nodeInfo = WonRdfUtils.WonNodeUtils.getWonNodeInfo(URI.create(dsWonNodeUri), ds);
        if (nodeInfo == null) {
            throw new DataIntegrityException("Could not load won node info from dataset with URI: " + dsWonNodeUri);
        }
        return nodeInfo;
    }

    /**
     * Get the won node URI from a {@link Dataset}
     *
     * @param ds Dataset which holds won node information
     * @return
     */
    private String getWonNodeUriFromDataset(Dataset ds) {
        if (ds.listNames().hasNext()) {
            Model model = ds.getNamedModel(ds.listNames().next());
            if (model.listSubjectsWithProperty(WON.uriPrefixSpecification).hasNext()) {
                return model.listSubjectsWithProperty(WON.uriPrefixSpecification).nextResource().toString();
            }
        }
        return null;
    }
}
