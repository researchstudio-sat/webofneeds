package common.service;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.expr.nodevalue.NodeValueBoolean;
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemote;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Iterator;

/**
 * Service to access of Sparql enpoint database to save or query linked data.
 *
 * User: hfriedrich
 * Date: 15.04.2015
 */
@Component
public class SparqlService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());
  protected String sparqlEndpoint;

  @Autowired
  public SparqlService(@Value("${uri.sparql.endpoint}")  String sparqlEndpoint) {
    this.sparqlEndpoint = sparqlEndpoint;
  }

  /**
   * Update named graph by first deleting it and afterwards inserting the triples of the new model.
   *
   * @param graph named graph to be updated
   * @param model model that holds triples to set
   */
  public void updateNamedGraph(String graph, Model model) {

    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, model, Lang.NTRIPLES);
    String query = "\nCLEAR GRAPH <" + graph + ">;\n" + "\nINSERT DATA { GRAPH <" + graph + "> { " + sw + "}};\n";
    executeUpdateQuery(query);
  }

  /**
   * Update a dataset of names graphs first deleting them and afterwards inserting the triples of the new models.
   *
   * @param ds
   */
  public void updateNamedGraphsOfDataset(Dataset ds) {

    Iterator<String> graphNames = ds.listNames();
    while (graphNames.hasNext()) {

      log.debug("Save dataset");
      String graphName = graphNames.next();
      Model model = ds.getNamedModel(graphName);
      updateNamedGraph(graphName, model);
    }
  }

  public Dataset retrieveDataset(String graphName) {

    DatasetGraph dsg = TDBFactory.createDatasetGraph();
    dsg.getContext().set(TDB.symUnionDefaultGraph, new NodeValueBoolean(true));
    Dataset ds = DatasetFactory.create(dsg);
    String queryTemplate = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <%s> { ?s ?p ?o } . }";
    String queryString = String.format(queryTemplate, graphName);
    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
    Model model = qexec.execConstruct();
    ds.addNamedModel(graphName, model);
    return ds;
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
