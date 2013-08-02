/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.matcher.query;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;
import org.sindice.siren.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.solr.SolrFields;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.io.StringReader;

/**
 * User: fkleedorfer
 * Date: 30.07.13
 */
public class TriplesQuery extends AbstractQuery
{
  private String field;
    private Logger logger = LoggerFactory.getLogger(getClass());


    public TriplesQuery(final BooleanClause.Occur occur, final String field)
  {
    super(occur);
    this.field = field;
  }

  @Override
  public Query getQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    logger.debug("creating exact triples query");
    String triples = (String) inputDocument.getFieldValue(field);
    logger.debug("plain ntriples field value: {}",triples);
    String docURI = (String) inputDocument.getFieldValue(SolrFields.URL);
    logger.debug("docURI: {}", docURI);
    Model model = toModel(triples);
    logger.debug("ntriples converted to jena model: {}", model);
    //identify the node where the free description starts
    Resource needNode = model.getResource(docURI);
    logger.debug("needNode:{}", needNode);
    if (needNode == null) return null;
    Resource contentNode = needNode.getPropertyResourceValue(WON.HAS_CONTENT);
    logger.debug("contentNode:{}", contentNode);
    if (contentNode == null) return null;
    Resource contentDescriptionNode = contentNode.getPropertyResourceValue(WON.HAS_CONTENT_DESCRIPTION);
    logger.debug("contentDescriptionNode:{}", contentDescriptionNode);
    if (contentDescriptionNode == null) return null;

    //extract the subgraph containing the free description
    Graph graph = model.getGraph();
    GraphExtract graphExtract = new GraphExtract(TripleBoundary.stopNowhere);
    Graph subGraph = graphExtract.extract(contentDescriptionNode.asNode(),graph);
    logger.debug("extracted a subgraph of size {} from model of size {}", subGraph.size(), graph.size());

    //create and return the siren query
    return createQueryForGraph(subGraph);
  }

    private Query createQueryForGraph(Graph graph) {
        BooleanQuery query = new BooleanQuery();
        ExtendedIterator<Triple> tripleIterator =  graph.find(null, null, null);
        while (tripleIterator.hasNext()){
            Triple triple = tripleIterator.next();
            query.add(createQueryForTriple(triple), BooleanClause.Occur.SHOULD);
        }
        logger.debug("created this boolean query:{}", query);
        return query;
    }

    private Query createQueryForTriple(Triple triple) {
        //for s,p,o of the triple, we create a SirenTermQuery with its value, wrapped in a SirenBooleanQuery with Occur.MUST
        // and put it into a SirenCellQuery (one for each of s, p, and o)
        // then, we aggregate the three SirenCellQueries into a SirenTupleQuery

        // Create a tuple query that combines the two cell queries
        final SirenTupleQuery tq = new SirenTupleQuery();

        //subject
        if (!triple.getSubject().isBlank()){
            final SirenBooleanQuery bq1 = new SirenBooleanQuery();
            bq1.add(new SirenTermQuery(
                        new Term(this.field, toQuotedString(triple.getSubject()))),
                        SirenBooleanClause.Occur.SHOULD);
            final SirenCellQuery cq1 = new SirenCellQuery(bq1);
            cq1.setConstraint(0);
            tq.add(cq1, SirenTupleClause.Occur.SHOULD);
        }

        //predicate (could it ever be a blank node??)
        if (!triple.getPredicate().isBlank()){
            final SirenBooleanQuery bq2 = new SirenBooleanQuery();
            bq2.add(new SirenTermQuery(
                        new Term(this.field, toQuotedString(triple.getPredicate()))),
                        SirenBooleanClause.Occur.SHOULD);
            final SirenCellQuery cq2 = new SirenCellQuery(bq2);
            cq2.setConstraint(1);
            tq.add(cq2, SirenTupleClause.Occur.SHOULD);
        }
        //object
        if (!triple.getObject().isBlank()){
            final SirenBooleanQuery bq3 = new SirenBooleanQuery();
            bq3.add(new SirenTermQuery(
                        new Term(this.field, toQuotedString(triple.getObject()))),
                        SirenBooleanClause.Occur.SHOULD);
            final SirenCellQuery cq3 = new SirenCellQuery(bq3);
            cq3.setConstraint(2);
            tq.add(cq3, SirenTupleClause.Occur.SHOULD);
        }
        return tq;
    }

    /**
     * adds <..> around an URI.
     * @param node
     * @return
     */
    private String toQuotedString(Node node){
      if (node.isURI()) return "<" + node.toString() + ">";
      return node.toString();
    }

    private Model toModel(String fieldValue) {
    Model model = ModelFactory.createDefaultModel();
    model.read(new StringReader(fieldValue), "", "N3");
    return model;
  }
}
