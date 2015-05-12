package node.service;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import commons.service.SparqlService;
import won.protocol.vocabulary.WON;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sparql service extended with methods for won node controller
 *
 * User: hfriedrich
 * Date: 04.05.2015
 */
public class WonNodeSparqlService extends SparqlService
{
  private final static String URI_PREFIX_SPECIFICATION = WON.BASE_URI + "hasUriPrefixSpecification";


  public WonNodeSparqlService(final String sparqlEndpoint) {
    super(sparqlEndpoint);
  }

  /**
   * Retrieve won node URIs that are saved in the Sparql endpoint
   *
   * @return Set of all known won node URIs
   */
  public Set<String> retrieveWonNodeUris() {

    Set<String> wonNodeUris = new LinkedHashSet<>();
    String queryTemplate = "\nSELECT ?node WHERE { GRAPH ?g {?node <%s> ?c}}\n";
    String queryString = String.format(queryTemplate, URI_PREFIX_SPECIFICATION);
    log.debug("Query SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", queryString);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
    ResultSet results = qexec.execSelect();

    while (results.hasNext()) {

      QuerySolution qs = results.nextSolution();
      RDFNode uri = qs.get("node");
      wonNodeUris.add(uri.toString());
    }
    qexec.close();

    return wonNodeUris;
  }
}
