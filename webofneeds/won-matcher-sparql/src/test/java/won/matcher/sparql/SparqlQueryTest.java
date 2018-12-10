package won.matcher.sparql;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_StrContains;
import org.apache.jena.sparql.expr.E_StrLowerCase;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.tdb.TDB;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mongodb.core.aggregation.BucketAutoOperation.ExpressionBucketAutoOperationBuilder;

import won.matcher.sparql.actor.SparqlMatcherActor;
import won.matcher.sparql.actor.SparqlMatcherUtils;
import won.protocol.vocabulary.WON;

/**
 * Test for experimenting with in-memory datasets and queries.
 * @author fkleedorfer
 *
 */
public class SparqlQueryTest  {
    
    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
    
    private String getResourceAsString(String name) throws Exception {
        byte[] buffer = new byte[256];
        StringWriter sw = new StringWriter();
        try (InputStream in = getResourceAsStream(name)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead = 0;
            while ((bytesRead = in.read(buffer)) > -1) {
                baos.write(buffer,0,bytesRead);
            }
            return new String(baos.toByteArray(),Charset.defaultCharset());
        }
        
    }
    
    @Test
    @Ignore // useful for trying things out, does not make so much sense as a unit test
    public void testQuery() throws Exception {
        Dataset dataset = DatasetFactory.create();
        RDFDataMgr.read(dataset, getResourceAsStream("sparqlquerytest/need2.trig"), Lang.TRIG);
        String queryString = getResourceAsString("sparqlquerytest/jobquery-orig.rq");
        
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        
        Op transformed = SparqlMatcherUtils.hintForCounterpartQuery(queryOp, Var.alloc("result"), 10);
        
        
        System.out.println("query algebra: " + queryOp);
        System.out.println("transformed query algebra: " + transformed);
        System.out.println("\nDataset:");
        RDFDataMgr.write(System.out, dataset, Lang.TRIG);
        System.out.println("\nQuery:");

        query = OpAsQuery.asQuery(transformed);
        System.out.println(query);
        System.out.println("\nResult:");
        
        
        try (QueryExecution execution = QueryExecutionFactory.create(query, dataset)) {
            execution.getContext().set(TDB.symUnionDefaultGraph, true);
            
            ResultSet result = execution.execSelect();
            Set<String> resultNeeds = new HashSet<>();
            while (result.hasNext()) {
                QuerySolution solution = result.next();
                System.out.println("solution:" + solution);
                String foundNeedURI = solution.get("result").toString();
                resultNeeds.add(foundNeedURI);
            }
            System.out.println(resultNeeds);
        }
    }
    
    
    @Test
    public void testAddGraphOp() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/query.rq");
        String queryWithGraphClauseString = getResourceAsString("sparqlquerytest/query-with-graph-clause.rq");
        
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        Query expectedQueryWithGraphClauseString = QueryFactory.create(queryWithGraphClauseString);
        Op expectedQueryWithGraphClause = Algebra.compile(expectedQueryWithGraphClauseString);
        Op queryWithGraphClause = SparqlMatcherUtils.addGraphOp(queryOp, Optional.of("?g"));
        
        Assert.assertEquals("Adding graph op to query did not yield expected result", expectedQueryWithGraphClause , queryWithGraphClause);
    }

    
    
    @Test
    public void testRemoveServiceOpNoName() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/query2.rq");
        String queryWithGraphClauseString = getResourceAsString("sparqlquerytest/query2-without-service-clause.rq");
        
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        Query expectedQuery = QueryFactory.create(queryWithGraphClauseString);
        Op expectedOp = Algebra.compile(expectedQuery);
        Op actualOp = SparqlMatcherUtils.removeServiceOp(queryOp);
        Assert.assertEquals("Adding graph op to query did not yield expected result", expectedOp, actualOp);
    }
    
    @Test
    public void testRemoveServiceOpWithName() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/query2.rq");
        String queryWithGraphClauseString = getResourceAsString("sparqlquerytest/query2-without-service-clause.rq");
        
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        Query expectedQuery = QueryFactory.create(queryWithGraphClauseString);
        Op expectedOp = Algebra.compile(expectedQuery);
        Op actualOp = SparqlMatcherUtils.removeServiceOp(queryOp, Optional.of("http://www.bigdata.com/rdf/geospatial#search"));
        Assert.assertEquals("Adding graph op to query did not yield expected result", expectedOp, actualOp);
    }
    
    @Test
    public void testRemoveServiceOpWithOtherName() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/query2.rq");
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        Op expectedOp = queryOp;
        Op actualOp = SparqlMatcherUtils.removeServiceOp(queryOp, Optional.of("http://example.com/some-other-uri"));
        Assert.assertEquals("Adding graph op to query did not yield expected result", expectedOp, actualOp);
    }
    
    /**
     * Test that reads a query from a file and repeatedly runs noHintForCounterpartQuery and hintForCounterpartQuery from
     * SparqlMatcherUtils, checking that the desired result is obtained each time, i.e., the methods have no side effects. 
     */
    @Test
    public void testAddRequiredFlag_NoHintForCounterpart_Nosideeffects() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/query-searchstring.rq");
        String queryStringHintForCounterpartExpected = getResourceAsString("sparqlquerytest/query-searchstring-hintForCounterpart-expected.rq");
        String queryStringNoHintForCounterpartExpected = getResourceAsString("sparqlquerytest/query-searchstring-noHintForCounterpart-expected.rq");
        
        Var resultName = Var.alloc("result");
        
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        
        Op actualOp = SparqlMatcherUtils.noHintForCounterpartQuery(queryOp, resultName, 30);
        Query expected = QueryFactory.create(queryStringNoHintForCounterpartExpected);
        Op expectedOp = Algebra.compile(expected);
        Assert.assertEquals(OpAsQuery.asQuery(expectedOp).toString(), OpAsQuery.asQuery(actualOp).toString());
        
        actualOp = SparqlMatcherUtils.hintForCounterpartQuery(queryOp, resultName, 30);
        expected = QueryFactory.create(queryStringHintForCounterpartExpected);
        expectedOp = Algebra.compile(expected);
        Assert.assertEquals(OpAsQuery.asQuery(expectedOp).toString(), OpAsQuery.asQuery(actualOp).toString());
        
        actualOp = SparqlMatcherUtils.noHintForCounterpartQuery(queryOp, resultName, 30);
        expected = QueryFactory.create(queryStringNoHintForCounterpartExpected);
        expectedOp = Algebra.compile(expected);
        Assert.assertEquals(OpAsQuery.asQuery(expectedOp).toString(), OpAsQuery.asQuery(actualOp).toString());
        
        actualOp = SparqlMatcherUtils.hintForCounterpartQuery(queryOp, resultName, 30);
        expected = QueryFactory.create(queryStringHintForCounterpartExpected);
        expectedOp = Algebra.compile(expected);
        Assert.assertEquals(OpAsQuery.asQuery(expectedOp).toString(), OpAsQuery.asQuery(actualOp).toString());
        
    }
  
}
