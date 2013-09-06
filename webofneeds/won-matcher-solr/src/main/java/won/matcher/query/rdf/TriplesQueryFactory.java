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

package won.matcher.query.rdf;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphExtract;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.TripleBoundary;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpAsQuery;
import com.hp.hpl.jena.sparql.algebra.Transformer;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.graph.NodeTransform;
import com.hp.hpl.jena.sparql.graph.NodeTransformLib;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;
import org.sindice.siren.search.SirenBooleanClause;
import org.sindice.siren.search.SirenBooleanQuery;
import org.sindice.siren.search.SirenTupleClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import won.matcher.query.AbstractQueryFactory;
import won.matcher.query.rdf.algebra.OpToSirenQuery;
import won.matcher.query.rdf.algebra.QueryCleaner;
import won.matcher.query.rdf.algebra.RemoveUnusedVars;
import won.protocol.solr.SolrFields;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: fkleedorfer
 * Date: 30.07.13
 */
public class TriplesQueryFactory extends AbstractQueryFactory
{
  private static final Pattern PATTERN_REMOVE_QUOTES = Pattern.compile("\"(.+)\".*");
  private static final BooleanClause.Occur DEFAULT_OCCUR = BooleanClause.Occur.SHOULD;
  private static final SirenBooleanClause.Occur OCCUR_LITERAL = SirenBooleanClause.Occur.SHOULD;
  private static final SirenTupleClause.Occur OCCUR_TRIPLE = SirenTupleClause.Occur.SHOULD;
  private static final BooleanClause.Occur OCCUR_STAR = BooleanClause.Occur.SHOULD;
  private static final BooleanClause.Occur OCCUR_ALL = BooleanClause.Occur.SHOULD;



  private String field;
    private Logger logger = LoggerFactory.getLogger(getClass());

  static {
    // Register system functions (such as sp:gt (>))
    SPINModuleRegistry.get().init();
  }


    public TriplesQueryFactory(final BooleanClause.Occur occur, float boost, final String field)
  {
    super(occur, boost);
    this.field = field;
  }

  public TriplesQueryFactory(final BooleanClause.Occur occur, final String field)
  {
    this(occur, 1.0f, field);
    this.field = field;
  }

  @Override
  public Query createQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    logger.debug("creating exact triples query");
    String triples = (String) inputDocument.getFieldValue(field);
    if (triples == null) {
      logger.debug("no triples found, omitting triples query");
      return null;
    }
    logger.debug("plain ntriples field value: {}",triples);
    String docURI = (String) inputDocument.getFieldValue(SolrFields.URL);
    logger.debug("docURI: {}", docURI);
    Model model = toModel(triples);
    logger.debug("ntriples converted to jena model: {}", model);
    SubModel subModelWithRoot = extractRelevantSubgraph(docURI, model);
    if (subModelWithRoot == null) return null;
    //create and return the siren query
    return createQueryForGraph(subModelWithRoot);
    //return createTestQuery();
  }

  /**
   * Identifies the subgraph containing the free content description, isolates it and returns it.
   * @param docURI
   * @param model
   * @return
   */
  private SubModel extractRelevantSubgraph(final String docURI, final Model model)
  {
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
    Model m = ModelFactory.createModelForGraph(subGraph);
    return new SubModel(m,model.listStatements(contentNode,WON.HAS_CONTENT_DESCRIPTION, contentDescriptionNode).next());
  }

  /**
   * Extracts all subgraphs from the specified model that are attached to it via won:attachSpinWhereClause
   * and returns a ModelWithRoot collection mapping the subject of these triples to the subgraphs attached to them.
   * @param model
   * @return
   */
  private Collection<SubModel> extractSPINSubgraphs(final Model model){
    StmtIterator triplesWithWhere = model.listStatements(null, WON.EMBED_SPIN_ASK, (RDFNode) null);
    List<SubModel> spinWhereClauses = new ArrayList<SubModel>();
    while(triplesWithWhere.hasNext()) {
      Statement attachingTriple = triplesWithWhere.next();
      //TODO we run into trouble if the SPIN expression references back into the non-SPIN graph when we use stopNowhere!
      GraphExtract graphExtract = new GraphExtract(TripleBoundary.stopNowhere);
      Graph subGraph = graphExtract.extract(attachingTriple.getObject().asNode(),model.getGraph());
      Model m = ModelFactory.createModelForGraph(subGraph);
      spinWhereClauses.add(new SubModel(m,attachingTriple));
    }
    return spinWhereClauses;
  }


  /**
   * Creates a Lucene (siren) query for the specified ModelWithRoot object.
   * @param modelWithRoot
   * @return
   */
  private Query createQueryForGraph(SubModel modelWithRoot) {
    Model graphWithoutSpinQueries = ModelFactory.createDefaultModel();
    //create a copy of the specified model so that we can modify the contents without side-effect
    graphWithoutSpinQueries.add(modelWithRoot.getModel());

    //extract all attached SPIN ask queries as graphs
    Collection<SubModel> spinQueryGraphs = extractSPINSubgraphs(modelWithRoot.getModel());
    if (spinQueryGraphs.size() > 0) {
      logger.debug("identified {} SPIN ask queries", spinQueryGraphs.size());
      //remove all triples that are inside the where clauses from the original model
      for (SubModel toRemoveFromMain: spinQueryGraphs){
        graphWithoutSpinQueries.remove(toRemoveFromMain.getModel());
      }
      if (logger.isDebugEnabled()){
        logger.debug("graph without SPIN ask queries:");
        for (StmtIterator it = graphWithoutSpinQueries.listStatements();it.hasNext();){
          logger.debug("{}",it.next());
        }
      }
      //remove all attachment triples as well
      graphWithoutSpinQueries.remove(graphWithoutSpinQueries.listStatements(null, WON.EMBED_SPIN_ASK, (RDFNode) null));


      logger.debug("size of content description graph without SPIN where clauses: {}", graphWithoutSpinQueries.size());
    }

    Op jenaOp = createOpForGraph(graphWithoutSpinQueries);
    jenaOp = attachSpinQueriesToOp(jenaOp, spinQueryGraphs);

    BooleanQuery query = new BooleanQuery();
    Iterator<Op> starShapedSubQueryOpIterator = new StarShapedSubqueryIterator(jenaOp);
    while (starShapedSubQueryOpIterator.hasNext()){
      Op starShapedSubQueryOp = starShapedSubQueryOpIterator.next();
      starShapedSubQueryOp = cleanupQuery(starShapedSubQueryOp);
      logger.debug("starshaped subquery op: {}", starShapedSubQueryOp);
      Map<String, SirenBooleanQuery> variableRestrictions = new HashMap<String, SirenBooleanQuery>();
      Query starShapedBooleanQuery = OpToSirenQuery.createQuery(starShapedSubQueryOp, this.field);
      query.add(starShapedBooleanQuery, BooleanClause.Occur.SHOULD);
      logger.debug("created this siren query for star shaped subquery: {}",starShapedBooleanQuery);
    }



    /*



    //build query for non-SPIN model:
    // * iterate over subjects, collect all incoming and outgoing RDF links
    // * build a star-shaped SirenBooleanQuery for the subject
    // * collect all such queries in a map indexed by subject
    // * aggregate all queries in one boolean query

    ResIterator subjectIterator = graphWithoutSpinQueries.listSubjects();
    while (subjectIterator.hasNext()){
      Resource subject = subjectIterator.next();
      logger.debug("creating star-shaped subquery for subject: {}",subject);
      Query starShapedQuery = createStarShapedQueryForRdfNode(subject, graphWithoutSpinQueries);
      logger.debug("query for subject: {}", starShapedQuery);
      query.add(starShapedQuery,
          OCCUR_STAR);
    }


//    //build queries for SPIN graphs
//    for(ModelWithRoot spinWhereClauseGraph: spinWhereClauseGraphs){
//      Query queryForSpinWhereClause = createQueryForSpinWhereClause(spinWhereClauseGraph);
//      query.add(queryForSpinWhereClause, BooleanClause.Occur.SHOULD);
//    }

    */

    logger.debug("created this query:{}", query);
    return query;
  }

  private Op cleanupQuery(final Op jenaOp)
  {
    Op cleaned = Transformer.transform( new QueryCleaner(), jenaOp);
    return Transformer.transform( new RemoveUnusedVars(), cleaned);
  }

  private Op attachSpinQueriesToOp(final Op jenaOp, final Collection<SubModel> spinQueryGraphs)
  {
    Op result = jenaOp;
    logger.debug("extending this op query: {}", jenaOp);
    //for each spin Query Graph, replace ?this with the "root"
    //then merge with the specified op
    for (SubModel modelWithRoot:spinQueryGraphs){
      modelWithRoot = addSpinModel(modelWithRoot);
      //get the query in SPIN API
      Resource instance = modelWithRoot.getModel().listSubjectsWithProperty(RDF.type, SP.Ask).nextResource();
      org.topbraid.spin.model.Query spinQuery = SPINFactory.asQuery(instance);
      //replace ?this
      logger.debug("extracted this spin query: {}", spinQuery);
      //convert to ARQ query
      com.hp.hpl.jena.query.Query jenaQuery = ARQFactory.get().createQuery(spinQuery);
      //now convert to Op
      Op subOp = Algebra.compile(jenaQuery);
      logger.debug("extracted query as op: {}", subOp);
      //replace the ?this variable with the node the query was attached to
      final Node thisReplacement = modelWithRoot.getAttachingStatement().getSubject().asNode();
      subOp = NodeTransformLib.transform(new NodeTransform()
      {
        @Override
        public Node convert(final Node node)
        {
          if (node.isVariable() && node.getName().equals(SPIN.THIS_VAR_NAME)) return thisReplacement;
          return node;
        }
      }, subOp);
      logger.debug("query with ?this replaced: {}", subOp);
      //merge with main
      result = OpJoin.createReduce(result,subOp);
    }
    com.hp.hpl.jena.query.Query q = OpAsQuery.asQuery(result);
    q.setQueryAskType();
    logger.debug("final query: {}", q);
    return result;
  }

  private SubModel addSpinModel(final SubModel subModel)
  {
    Model model = subModel.getModel();
    if(!model.contains(SPIN._arg1, RDF.type, SP.Variable)) {
      MultiUnion multiUnion = JenaUtil.createMultiUnion(new Graph[]{
          model.getGraph(),
          SPIN.getModel().getGraph()
      });
       return new SubModel( ModelFactory.createModelForGraph(multiUnion),subModel.getAttachingStatement());
    } else {
      return subModel;
    }
  }

  private Op createOpForGraph(final Model graphWithoutSpinQueries)
  {
    BasicPattern pat = new BasicPattern();                 // Make a pattern
    for (StmtIterator it = graphWithoutSpinQueries.listStatements(); it.hasNext();){
      pat.add(it.next().asTriple());
    }
    return new OpBGP(pat);                                   // Make a BGP from this pattern
  }

  /*
  private SirenTupleQuery createStarShapedQueryForRdfNode(Resource center, Model model){
    StmtIterator statements = model.listStatements(center, null,(RDFNode) null);
    SirenTupleQuery starQuery = new SirenTupleQuery();
    while (statements.hasNext()){
      Statement statement = statements.next();
      logger.debug("filling tuplequery from triple {}",statement);
      fillTupleQueryFromTriple(statement, starQuery);
    }
    return starQuery;
  }
  */


  /*
  private Query createQueryForSpinWhereClause(final ModelWithRoot spinWhereClauseGraph)
  {
    ElementVisitor elementVisitor = new ElementVisitor

    ElementWalker elementWalker = new ElementWalker(elementVisitor, expressionVisitor);
  }
  */

  /*
  private Query createQueryForTriple(Statement triple) {
        //for s,p,o of the triple, we create a SirenTermQuery with its value, wrapped in a SirenBooleanQuery with Occur.MUST
        // and put it into a SirenCellQuery (one for each of s, p, and o)
        // then, we aggregate the three SirenCellQueries into a SirenTupleQuery

        // Create a tuple query that combines the two cell queries
    final SirenTupleQuery tq = new SirenTupleQuery();
    fillTupleQueryFromTriple(triple, tq);
    return tq;
  } */
  /*
  private void fillTupleQueryFromTriple(final Statement triple, final SirenTupleQuery tq)
  {
    //subject
    if (!triple.getSubject().isAnon()){
        final SirenBooleanQuery bq1 = new SirenBooleanQuery();
        bq1.add(new SirenTermQuery(
                    new Term(this.field, toQuotedString(triple.getSubject()))),
                    OCCUR_LITERAL);
        final SirenCellQuery cq1 = new SirenCellQuery(bq1);
        cq1.setConstraint(0);
        tq.add(cq1, OCCUR_TRIPLE);
    }

    //predicate (could it ever be a blank node??)
    if (!triple.getPredicate().isAnon()){
        final SirenBooleanQuery bq2 = new SirenBooleanQuery();
        bq2.add(new SirenTermQuery(
                    new Term(this.field, toQuotedString(triple.getPredicate()))),
                    OCCUR_LITERAL);
        final SirenCellQuery cq2 = new SirenCellQuery(bq2);
        cq2.setConstraint(1);
        tq.add(cq2, OCCUR_TRIPLE);
    }
    //object
    if (!triple.getObject().isAnon()){
        final SirenBooleanQuery bq3 = new SirenBooleanQuery();
        bq3.add(new SirenTermQuery(
                    new Term(this.field, toQuotedString(triple.getObject()))),
                    OCCUR_LITERAL);
        final SirenCellQuery cq3 = new SirenCellQuery(bq3);
        cq3.setConstraint(2);
        tq.add(cq3, OCCUR_TRIPLE);
    }
  }
  */

  /**
     * adds <..> around an URI.
     * @param node
     * @return
     */
    private String toQuotedString(RDFNode node){
      if (node.isResource()) return node.toString().toLowerCase();
      if (node.isLiteral()) {
        String nodeAsString = node.toString().toLowerCase();
        Matcher matcher = PATTERN_REMOVE_QUOTES.matcher(nodeAsString);
        if (matcher.matches()){
          return matcher.group(1);
        }
        return nodeAsString;
      }
      return node.toString();
    }

    private Model toModel(String fieldValue) {
    Model model = ModelFactory.createDefaultModel();
    model.read(new StringReader(fieldValue), "", "N3");
    return model;
  }

  /**
   * Wrapper around model to hold a subgraph extracted from a bigger graph along with the statement that connected
   * the subgraph with the bigger graph.
   */
  private class SubModel
  {
    private Model model;
    private Statement attachingStatement;

    public Model getModel()
    {
      return model;
    }

    public Statement getAttachingStatement()
    {
      return attachingStatement;
    }

    private SubModel(final Model model, final Statement attachingStatement)
    {
      this.model = model;
      this.attachingStatement = attachingStatement;
    }
  }
}
