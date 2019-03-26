/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.owner.protocol.message.base;

import java.net.URI;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.tdb.TDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.owner.protocol.message.OwnerCallback;
import won.protocol.exception.DataIntegrityException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Match;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

/**
 * Implementation of the WonMessageHandlerAdapter that uses a Dataset for
 * creating the objects needed for invoking the adaptee's callback methods.
 * <p/>
 * Sent and received messages are added to the dataset automatically. Missing
 * data is automatically loaded via linked data.
 */
public class DatasetBackedOwnerCallbackAdapter extends OwnerCallbackAdapter {
  // TODO move to the queries object!
  private static final String QUERY_CONNECTION = "SELECT ?con ?need ?state ?remoteCon ?remoteNeed ?type where { "
      + "  ?con won:belongsToNeed ?need; " + "     won:isInState ?state; " + "     won:hasFacet ?type; "
      + "     won:hasRemoteNeed ?remoteNeed." + "  OPTIONAL { " + "    ?con won:hasRemoteConnection ?remoteCon" + "  } "
      + "} ";
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private Dataset dataset;

  @Autowired
  private LinkedDataSource linkedDataSource;

  public DatasetBackedOwnerCallbackAdapter(final OwnerCallback adaptee) {
    super(adaptee);
  }

  @Override
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
    RdfUtils.addDatasetToDataset(dataset, message.getCompleteDataset());
    return super.process(message);
  }

  public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
    this.linkedDataSource = linkedDataSource;
  }

  public void setDataset(final Dataset dataset) {
    this.dataset = dataset;
  }

  @Override
  protected Connection makeConnection(final WonMessage wonMessage) {
    URI connUri = wonMessage.getReceiverURI();
    ParameterizedSparqlString pss = new ParameterizedSparqlString();
    pss.setNsPrefix("won", WON.BASE_URI);
    pss.setCommandText(QUERY_CONNECTION);
    pss.setIri("con", connUri.toString());
    Query query = pss.asQuery();
    try (QueryExecution qExec = QueryExecutionFactory.create(query, dataset)) {
      qExec.getContext().set(TDB.symUnionDefaultGraph, true);
      Connection con = null;
      final ResultSet results = qExec.execSelect();
      if (results.hasNext()) {
        QuerySolution soln = results.next();
        if (results.hasNext()) {
          throw new DataIntegrityException("Query must not yield multiple solutions");
        }
        con = new Connection();
        con.setConnectionURI(getURIFromSolution(soln, "con"));
        con.setTypeURI(getURIFromSolution(soln, "type"));
        con.setNeedURI(getURIFromSolution(soln, "need"));
        con.setState(ConnectionState.fromURI(getURIFromSolution(soln, "state")));
        con.setRemoteNeedURI(getURIFromSolution(soln, "remoteNeed"));
        con.setRemoteConnectionURI(getURIFromSolution(soln, "remoteCon"));
      }
      return con;
    }
  }

  private URI getURIFromSolution(final QuerySolution soln, String var) {
    return URI.create(soln.getResource(var).getURI().toString());
  }

  @Override
  protected Match makeMatch(final WonMessage wonMessage) {
    return WonRdfUtils.MessageUtils.toMatch(wonMessage);
  }
}
