package commons.service;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemote;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.io.StringWriter;
import java.util.Iterator;

/**
 * Service to access of Sparql enpoint database to save or query linked data.
 *
 * User: hfriedrich
 * Date: 15.04.2015
 */
public class SparqlService
{
  private static final String METADATA_GRAPH = WON.BASE_URI + "crawlMetadata";
  private static final String CRAWL_DATE_PREDICATE = WON.BASE_URI + "crawlDate";
  private static final String CRAWL_STATUS_PREDICATE = WON.BASE_URI + "crawlStatus";
  private static final String CRAWL_BASE_URI_PREDICATE = WON.BASE_URI + "crawlBaseUri";

  protected final Logger log = LoggerFactory.getLogger(getClass());
  protected String sparqlEndpoint;

  public SparqlService(String sparqlEndpoint) {
    this.sparqlEndpoint = sparqlEndpoint;
  }

  /**
   * Update a graph by first deleting it and afterwards inserting the triples of the new model.
   *
   * @param graph graph to be updated
   * @param model model that holds triples to set
   */
  public void updateGraph(String graph, Model model) {

    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, model, Lang.NTRIPLES);
    String query = "\nCLEAR GRAPH <" + graph + ">;\n" + "\nINSERT DATA { GRAPH <" + graph + "> { " + sw + "}};\n";
    executeUpdateQuery(query);
  }

  /**
   * Update a dataset of graphs first deleting them and afterwards inserting the triples of the new models.
   *
   * @param ds
   */
  public void updateDataset(Dataset ds) {

    Iterator<String> graphNames = ds.listNames();
    while (graphNames.hasNext()) {

      log.debug("Save dataset");
      String graphName = graphNames.next();
      Model model = ds.getNamedModel(graphName);
      updateGraph(graphName, model);
    }
  }

  /**
   * Execute a SPARQL Update query.
   *
   * @param updateQuery
   */
  public void executeUpdateQuery(String updateQuery) {

    log.debug("Update SPARQL Endpoint: {}", sparqlEndpoint);
    log.debug("Execute query: {}", updateQuery);
    UpdateRequest query = UpdateFactory.create(updateQuery);
    UpdateProcessRemote riStore = (UpdateProcessRemote)
      UpdateExecutionFactory.createRemote(query, sparqlEndpoint);
    riStore.execute();
  }

}
