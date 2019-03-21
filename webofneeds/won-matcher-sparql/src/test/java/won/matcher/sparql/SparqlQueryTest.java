package won.matcher.sparql;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

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
import org.apache.jena.sparql.core.Var;
import org.apache.jena.tdb.TDB;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import won.matcher.sparql.actor.SparqlMatcherUtils;

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
        
        Op transformed = SparqlMatcherUtils.hintForCounterpartQuery(queryOp, Var.alloc("result"));
        
        
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
        String queryString = getResourceAsString("sparqlquerytest/no-hint-for-counterpart/input.rq");
        String queryStringHintForCounterpartExpected = getResourceAsString("sparqlquerytest/hint-for-counterpart/expected.rq");
        String queryStringNoHintForCounterpartExpected = getResourceAsString("sparqlquerytest/no-hint-for-counterpart/expected.rq");
        
        Var resultName = Var.alloc("result");
        Var scoreName = Var.alloc("score");
        
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        
        Op actualOp = SparqlMatcherUtils.noHintForCounterpartQuery(queryOp, resultName);
        Query expected = QueryFactory.create(queryStringNoHintForCounterpartExpected);
        Op expectedOp = Algebra.compile(expected);
        Assert.assertEquals(OpAsQuery.asQuery(expectedOp).toString(), OpAsQuery.asQuery(actualOp).toString());
        
        actualOp = SparqlMatcherUtils.hintForCounterpartQuery(queryOp, resultName);
        expected = QueryFactory.create(queryStringHintForCounterpartExpected);
        expectedOp = Algebra.compile(expected);
        Assert.assertEquals(OpAsQuery.asQuery(expectedOp).toString(), OpAsQuery.asQuery(actualOp).toString());
        
        actualOp = SparqlMatcherUtils.noHintForCounterpartQuery(queryOp, resultName);
        expected = QueryFactory.create(queryStringNoHintForCounterpartExpected);
        expectedOp = Algebra.compile(expected);
        Assert.assertEquals(OpAsQuery.asQuery(expectedOp).toString(), OpAsQuery.asQuery(actualOp).toString());
        
        actualOp = SparqlMatcherUtils.hintForCounterpartQuery(queryOp, resultName);
        expected = QueryFactory.create(queryStringHintForCounterpartExpected);
        expectedOp = Algebra.compile(expected);
        Assert.assertEquals(OpAsQuery.asQuery(expectedOp).toString(), OpAsQuery.asQuery(actualOp).toString());
        
    }
 

    @Test
    public void testAddRequiredFlag_NoHintForCounterpart() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/no-hint-for-counterpart/input.rq");
        String expectedQueryString = getResourceAsString("sparqlquerytest/no-hint-for-counterpart/expected.rq");
        Var resultName = Var.alloc("result");
        Var scoreName = Var.alloc("score");
        checkResult(queryString, expectedQueryString, op -> SparqlMatcherUtils.noHintForCounterpartQuery(op, resultName));
    }
    
    @Test
    public void testAddRequiredFlag_hintForCounterpart() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/hint-for-counterpart/input.rq");
        String expectedQueryString = getResourceAsString("sparqlquerytest/hint-for-counterpart/expected.rq");
        Var resultName = Var.alloc("result");
        Var scoreName = Var.alloc("score");
        checkResult(queryString, expectedQueryString, op -> SparqlMatcherUtils.hintForCounterpartQuery(op, resultName));
    }
    
    @Test
    public void testAddRequiredFlag_hintForCounterpart_complex1() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/nhfc-complex-query1/input.rq");
        String expectedQueryString = getResourceAsString("sparqlquerytest/nhfc-complex-query1/expected.rq");
        Var resultName = Var.alloc("result");
        Var scoreName = Var.alloc("score");
        checkResult(queryString, expectedQueryString, op -> SparqlMatcherUtils.noHintForCounterpartQuery(op, resultName));
    }
    
    @Test
    public void testAddExcludedFlag_NoHintForCounterpart_Whatsaround() throws Exception {
        String queryString = getResourceAsString("sparqlquerytest/whatsaround_excludeflag_NHFC/input.rq");
        String expectedQueryString = getResourceAsString("sparqlquerytest/whatsaround_excludeflag_NHFC/expected.rq");
        Var resultName = Var.alloc("result");
        Var scoreName = Var.alloc("score");
        checkResult(queryString, expectedQueryString, op -> SparqlMatcherUtils.hintForCounterpartQuery(op, resultName));
    }

    @Test
    public void testSearchQuery() throws Exception {
        String expectedQueryString = getResourceAsString("sparqlquerytest/searchstring-query/expected.rq");
        Var resultName = Var.alloc("result");
        Var scoreName = Var.alloc("score");
        Op actualOp = SparqlMatcherUtils.createSearchQuery("The Query String", resultName, 3, false, false);
        
        Query expectedQuery = QueryFactory.create(expectedQueryString);
        Op expectedOp = Algebra.compile(expectedQuery);
        String actual = OpAsQuery.asQuery(actualOp).toString();
        String expected = OpAsQuery.asQuery(expectedOp).toString();
        Assert.assertEquals(expected, actual);
    }
    
    private void checkResult(String queryString, String expectedQueryString, Function<Op, Op> transform) {
        Query query = QueryFactory.create(queryString);
        Op queryOp = Algebra.compile(query);
        
        
        Op actualOp = transform.apply(queryOp);
        Query expectedQuery = QueryFactory.create(expectedQueryString);
        Op expectedOp = Algebra.compile(expectedQuery);
        String actual = OpAsQuery.asQuery(actualOp).toString();
        
        String expected = OpAsQuery.asQuery(expectedOp).toString();
        /*
        System.out.println("expected:");
        System.out.println(expectedOp);
        System.out.println(expected);
        System.out.println("actual:");
        System.out.println(actualOp);
        System.out.println(actual);
        */
        Assert.assertEquals(expected, actual);
    }
}
