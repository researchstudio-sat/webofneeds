package won.matcher.service.nodemanager.service;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import won.matcher.service.common.service.sparql.SparqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import won.protocol.exception.DataIntegrityException;
import won.protocol.service.WonNodeInfo;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Sparql service extended with methods for won node controller
 *
 * User: hfriedrich
 * Date: 04.05.2015
 */
@Component
public class WonNodeSparqlService extends SparqlService
{

  @Autowired
  public WonNodeSparqlService(@Value("${uri.sparql.endpoint}") final String sparqlEndpoint) {
    super(sparqlEndpoint);
  }

  /**
   * Retrieve resource data of all known won nodes that are saved in the Sparql endpoint.
   *
   * @return Set of all known won node resource data
   */
  public Set<WonNodeInfo> retrieveAllWonNodeInfo() {

    Set<WonNodeInfo> wonNodeInfos = new HashSet<>();
    String queryString = "SELECT ?graphUri ?nodeUri WHERE { GRAPH ?graphUri {?nodeUri <%s> ?c} }";
    queryString = String.format(queryString, WON.HAS_URI_PATTERN_SPECIFICATION.toString());
    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", queryString);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
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
    qexec.close();
    return wonNodeInfos;
  }

  /**
   * Get the {@link won.protocol.service.WonNodeInfo} as an object from a {@link com.hp.hpl.jena.query.Dataset}
   *
   * @param ds Dataset which holds won node information
   * @return
   */
  public WonNodeInfo getWonNodeInfoFromDataset(Dataset ds) {

    String dsWonNodeUri = getWonNodeUriFromDataset(ds);
    WonNodeInfo nodeInfo = WonRdfUtils.WonNodeUtils.getWonNodeInfo(URI.create(dsWonNodeUri), ds);
    if (nodeInfo == null) {
      throw new DataIntegrityException(
        "Could not load won node info from dataset with URI: " + dsWonNodeUri);
    }

    return nodeInfo;
  }

  /**
   * Get the won node URI from a {@link com.hp.hpl.jena.query.Dataset}
   *
   * @param ds Dataset which holds won node information
   * @return
   */
  private String getWonNodeUriFromDataset(Dataset ds) {

    if (ds.listNames().hasNext()) {
      Model model = ds.getNamedModel(ds.listNames().next());
      if (model.listSubjectsWithProperty(WON.HAS_URI_PATTERN_SPECIFICATION).hasNext()) {
        return model.listSubjectsWithProperty(WON.HAS_URI_PATTERN_SPECIFICATION).nextResource().toString();
      }
    }
    return null;
  }

}
