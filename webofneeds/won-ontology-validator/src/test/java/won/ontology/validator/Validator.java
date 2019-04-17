/*
 * Copyright 2013 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.ontology.validator;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit tests to check the WoN ontology against the competency questions. One
 * test case for each question.
 *
 * @author Fabian Salcher
 * @version 2013/04
 */
public class Validator {
    private final static String WON_ONTOLOGY_FILE = "/won_ontology_v0.11.rdf";
    private final static String EXAMPLE_ONTOLOGY_FILE = "/won_ontology_example_1.rdf";
    private final static String WON_ONTOLOGY_URI = "https://w3id.org/won/core#";
    private final static String EXAMPLE_ONTOLOGY_URI = "https://w3id.org/won/example#";
    private static OntModel ontModel;
    private static String sparqlPreface;

    @BeforeClass
    public static void loadOntologies() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        System.out.println("loading ontologies... ");
        ontModel.read(Validator.class.getResourceAsStream(WON_ONTOLOGY_FILE), null);
        ontModel.read(Validator.class.getResourceAsStream(EXAMPLE_ONTOLOGY_FILE), null);
        sparqlPreface = "" + "PREFIX won: <" + WON_ONTOLOGY_URI + ">" + "PREFIX wonexample: <" + EXAMPLE_ONTOLOGY_URI
                        + ">" + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
                        + "PREFIX gr: <http://purl.org/goodrelations/v1#>";
    }

    @Test
    public void testCQEvent1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?event ?time WHERE {?event rdf:type won:Hint. "
                        + "?event won:timeStamp ?time}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.getLiteral("?time").toString());
            }
        } finally {
            qExec.close();
        }
        assertEquals("wrong number of results", 1, actualList.size());
        String expectedTime = "2013-04-04T07:26:05+02:00^^http://www.w3.org/2001/XMLSchema#dateTimeStamp";
        assertThat(actualList, hasItems(expectedTime));
    }

    @Test
    public void testCQEvent2() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?event ?eventType WHERE {"
                        + "wonexample:MessageContainer_01 rdfs:member ?event. " + "?event rdf:type won:Event."
                        + "?event rdf:type ?eventType." + "?eventType rdfs:subClassOf won:Event." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        assertTrue("wrong number of results", actualList.size() >= 1);
        String expected1 = "( ?event = <" + EXAMPLE_ONTOLOGY_URI + "Open_01_1> ) ( ?eventType = <" + WON_ONTOLOGY_URI
                        + "Open> )";
        String expected2 = "( ?event = <" + EXAMPLE_ONTOLOGY_URI + "Hint_01_1> ) ( ?eventType = <" + WON_ONTOLOGY_URI
                        + "Hint> )";
        assertThat(actualList, hasItems(expected1, expected2));
    }

    @Test
    public void testCQEvent3() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?event ?originator WHERE {"
                        + "wonexample:MessageContainer_01 rdfs:member ?event. " + "?event rdf:type won:Event."
                        + "?event won:originator ?originator." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        assertEquals("wrong number of results", 2, actualList.size());
        String expected1 = "( ?event = <" + EXAMPLE_ONTOLOGY_URI + "Open_01_1> ) ( ?originator = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomOwner_1> )";
        String expected2 = "( ?event = <" + EXAMPLE_ONTOLOGY_URI + "Hint_01_1> ) ( ?originator = <"
                        + EXAMPLE_ONTOLOGY_URI + "Matcher_01> )";
        assertThat(actualList, hasItems(expected1, expected2));
    }

    @Test
    public void testCQHow1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface
                        + "SELECT ?atom ?atomModality ?price ?currency ?lowerPrice ?upperPrice WHERE {"
                        + "?atom rdf:type won:Atom. " + "?atom won:atomModality ?atomModality."
                        + "?atomModality won:priceSpecification ?price." + "?price won:currency ?currency."
                        + "?price won:lowerPriceLimit ?lowerPrice." + "?price won:upperPriceLimit ?upperPrice." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        String expected1 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?price = <" + EXAMPLE_ONTOLOGY_URI
                        + "PriceSpecification_01_1> ) "
                        + "( ?currency = \"EUR\"^^xsd:string ) ( ?lowerPrice = \"100.0\"^^xsd:float ) "
                        + "( ?upperPrice = \"100.0\"^^xsd:float )";
        String expected2 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_2> ) " + "( ?price = <" + EXAMPLE_ONTOLOGY_URI
                        + "PriceSpecification_01_2> ) "
                        + "( ?currency = \"EUR\"^^xsd:string ) ( ?lowerPrice = \"130.0\"^^xsd:float ) "
                        + "( ?upperPrice = \"130.0\"^^xsd:float )";
        assertThat(actualList, hasItems(expected1, expected2));
    }

    @Test
    public void testCQWhat1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?text ?description WHERE {" + "?atom rdf:type won:Atom. "
                        + "?atom won:content ?content." + "?content dc:description ?text."
                        + "?content won:contentDescription ?description." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        assertEquals("wrong number of results", 1, actualList.size());
        String expected = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) "
                        + "( ?text = \"rote Couch mit integrierter Mäusefalle\"^^xsd:string ) " + "( ?description = <"
                        + EXAMPLE_ONTOLOGY_URI + "MyCouch> )";
        assertThat(actualList, hasItems(expected));
    }

    @Test
    public void testCQWhat2() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?descType WHERE {" + "?atom rdf:type won:Atom. "
                        + "?atom won:content ?content." + "?content won:contentDescription ?description."
                        + "?description rdf:type ?descType." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        String expected = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) "
                        + "( ?descType = <http://www.freebase.com/m/02crq1> )";
        assertThat(actualList, hasItems(expected));
    }

    @Test
    public void testCQWhat4() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?height ?heightV ?heightM ?width ?widthV ?widthM "
                        + "?depth ?depthV ?depthM ?weight ?weightV ?weightM WHERE {" + "?atom rdf:type won:Atom. "
                        + "?atom won:content ?content." + "?content won:height ?height."
                        + "?height gr:hasValue ?heightV." + "?height gr:hasUnitOfMeasurement ?heightM."
                        + "?content won:width ?width." + "?width gr:hasValue ?widthV."
                        + "?width gr:hasUnitOfMeasurement ?widthM." + "?content won:depth ?depth."
                        + "?depth gr:hasValue ?depthV." + "?depth gr:hasUnitOfMeasurement ?depthM."
                        + "?content won:weight ?weight." + "?weight gr:hasValue ?weightV."
                        + "?weight gr:hasUnitOfMeasurement ?weightM." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        assertEquals("wrong number of results", 1, actualList.size());
        String expected = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?height = <" + EXAMPLE_ONTOLOGY_URI
                        + "CouchHeight> ) ( ?heightV = \"50\"^^rdfs:Literal ) "
                        + "( ?heightM = \"cm\"^^xsd:string ) ( ?width = <" + EXAMPLE_ONTOLOGY_URI + "CouchWidth> ) "
                        + "( ?widthV = \"250\"^^rdfs:Literal ) ( ?widthM = \"cm\"^^xsd:string ) " + "( ?depth = <"
                        + EXAMPLE_ONTOLOGY_URI + "CouchDepth> ) ( ?depthV = \"50\"^^rdfs:Literal ) "
                        + "( ?depthM = \"cm\"^^xsd:string ) ( ?weight = <" + EXAMPLE_ONTOLOGY_URI + "CouchWeight> ) "
                        + "( ?weightV = \"120\"^^rdfs:Literal ) ( ?weightM = \"kg\"^^xsd:string )";
        assertThat(actualList, hasItems(expected));
    }

    @Test
    public void testCQWhat6() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?height ?width ?depth ?weight WHERE {"
                        + "?atom rdf:type won:Atom. " + "?atom won:content ?content." + "?content won:height ?height."
                        + "?content won:width ?width." + "?content won:depth ?depth." + "?content won:weight ?weight."
                        + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        assertEquals("wrong number of results", 1, actualList.size());
        String expected = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?height = <" + EXAMPLE_ONTOLOGY_URI
                        + "CouchHeight> ) " + "( ?width = <" + EXAMPLE_ONTOLOGY_URI + "CouchWidth> ) " + "( ?depth = <"
                        + EXAMPLE_ONTOLOGY_URI + "CouchDepth> ) " + "( ?weight = <" + EXAMPLE_ONTOLOGY_URI
                        + "CouchWeight> )";
        assertThat(actualList, hasItems(expected));
    }

    @Test
    public void testCQWhen1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?creationDate WHERE {" + "?atom rdf:type won:Atom. "
                        + "?atom won:atomCreationDate ?creationDate." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        assertEquals("wrong number of results", 2, actualList.size());
        String expected1 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_02> ) "
                        + "( ?creationDate = \"2013-03-20T14:29:00+02:00\"^^xsd:dateTimeStamp )";
        String expected2 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) "
                        + "( ?creationDate = \"2013-04-04T07:26:00+02:00\"^^xsd:dateTimeStamp )";
        assertThat(actualList, hasItems(expected1, expected2));
    }

    @Test
    public void testCQWhen3() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?atomModality ?timeConstraint ?p ?o WHERE {"
                        + "?atom rdf:type won:Atom. " + "?atom won:atomModality ?atomModality."
                        + "?atomModality won:availableAtTime ?timeConstraint." + "?timeConstraint ?p ?o." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        String expected1 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_1_2> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "recurIn> ) ( ?o = \"P7D\"^^xsd:duration )";
        String expected2 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_1_2> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "endTime> ) ( ?o = \"2013-04-05T17:00:00+02:00\"^^xsd:dateTimeStamp )";
        String expected3 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_1_2> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "startTime> ) ( ?o = \"2013-04-05T09:00:00+02:00\"^^xsd:dateTimeStamp )";
        String expected4 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_1_2> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "recurTimes> ) ( ?o = 0 )";
        String expected5 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_1_1> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "recurIn> ) ( ?o = \"P7D\"^^xsd:duration )";
        String expected6 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_1_1> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "endTime> ) ( ?o = \"2013-04-04T17:00:00+02:00\"^^xsd:dateTimeStamp )";
        String expected7 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_1_1> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "startTime> ) ( ?o = \"2013-04-04T09:00:00+02:00\"^^xsd:dateTimeStamp )";
        String expected8 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_1_1> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "recurTimes> ) ( ?o = 0 )";
        String expected9 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_2> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_2> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "endTime> ) ( ?o = \"2013-05-04T09:00:00+02:00\"^^xsd:dateTimeStamp )";
        String expected10 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_2> ) " + "( ?timeConstraint = <"
                        + EXAMPLE_ONTOLOGY_URI + "TimeConstraint_01_2> ) " + "( ?p = <" + WON_ONTOLOGY_URI
                        + "startTime> ) ( ?o = \"2013-04-04T09:00:00+02:00\"^^xsd:dateTimeStamp )";
        assertThat(actualList, hasItems(expected1, expected2, expected3, expected4, expected5, expected6, expected7,
                        expected8, expected9, expected10));
    }

    @Test
    public void testCQWhere1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?atomModality ?location ?p ?o WHERE {"
                        + "?atom rdf:type won:Atom. " + "?atom won:atomModality ?atomModality."
                        + "?atomModality won:availableAtLocation ?location." + "?location ?p ?o." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        String expected1 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?location = <" + EXAMPLE_ONTOLOGY_URI
                        + "Thurngasse_8> ) " + "( ?p = <http://www.w3.org/2003/01/geo/wgs84_pos#latitude> ) "
                        + "( ?o = \"48.218746\"^^xsd:float )";
        String expected2 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_1> ) " + "( ?location = <" + EXAMPLE_ONTOLOGY_URI
                        + "Thurngasse_8> ) " + "( ?p = <http://www.w3.org/2003/01/geo/wgs84_pos#longitude> ) "
                        + "( ?o = \"16.360283\"^^xsd:float )";
        String expected3 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?atomModality = <"
                        + EXAMPLE_ONTOLOGY_URI + "AtomModality_01_2> ) " + "( ?location = <" + EXAMPLE_ONTOLOGY_URI
                        + "Österreich> ) " + "( ?p = <" + WON_ONTOLOGY_URI + "iSOCode> ) ( ?o = \"AT\"^^xsd:string )";
        assertThat(actualList, hasItems(expected1, expected2, expected3));
    }

    @Test
    public void testCQWhere2() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?location ?concealed WHERE {" + "?atom rdf:type won:Atom. "
                        + "?atom won:atomModality ?modality." + "?modality won:availableAtLocation ?location."
                        + "?location won:isConcealed ?concealed." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        assertEquals("wrong number of results", 2, actualList.size());
        String expected1 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?location = <"
                        + EXAMPLE_ONTOLOGY_URI + "Thurngasse_8> ) ( ?concealed = true )";
        String expected2 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?location = <"
                        + EXAMPLE_ONTOLOGY_URI + "Österreich> ) ( ?concealed = false )";
        assertThat(actualList, hasItems(expected1, expected2));
    }

    @Test
    public void testCQWho1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?owner WHERE {" + "?atom rdf:type won:Atom. "
                        + "?atom won:owner ?owner." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        assertEquals("wrong number of results", 1, actualList.size());
        String expected = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?owner = <" + EXAMPLE_ONTOLOGY_URI
                        + "AtomOwner_1> )";
        assertThat(actualList, hasItems(expected));
    }

    @Test
    public void testCQState1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?state WHERE {" + "?atom rdf:type won:Atom. "
                        + "?atom won:atomState ?state." + "?state rdf:type won:AtomState." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        String expected1 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_02> ) " + "( ?state = <" + WON_ONTOLOGY_URI
                        + "Active> )";
        String expected2 = "( ?atom = <" + EXAMPLE_ONTOLOGY_URI + "Atom_01> ) " + "( ?state = <" + WON_ONTOLOGY_URI
                        + "Active> )";
        assertThat(actualList, hasItems(expected1, expected2));
        assertEquals("wrong number of results", 2, actualList.size());
    }

    @Test
    public void testCQEventStatement1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?event ?additionalInfo WHERE {?event rdf:type won:Event. "
                        + "?event won:hasAdditionalInformation ?additionalInfo" + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        String expected1 = "( ?event = <https://w3id.org/won/example#Hint_01_1> ) "
                        + "( ?additionalInfo = <https://w3id.org/won/example#MatchExplanation_01> )";
        assertThat(actualList, hasItems(expected1));
        assertEquals("wrong number of results", 1, actualList.size());
    }

    @Test
    public void testCQConstraint1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?constraint WHERE {?atom rdf:type won:Atom. "
                        + "?atom won:hasMatchingConstraint ?constraint" + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        String expected1 = "( ?atom = <https://w3id.org/won/example#Atom_01> ) ( ?constraint = <https://w3id.org/won/example#MyCouchConstraint> )";
        assertThat(actualList, hasItems(expected1));
        assertEquals("wrong number of results", 1, actualList.size());
    }

    @Test
    public void testCQTag1() {
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?tag WHERE {?atom rdf:type won:Atom. "
                        + "?atom won:content ?content." + "?content won:tag ?tag." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        List<String> actualList = new ArrayList<String>();
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
        } finally {
            qExec.close();
        }
        String expected1 = "( ?atom = <https://w3id.org/won/example#Atom_01> ) "
                        + "( ?tag = <http://www.dmoz.org/Shopping/Home_and_Garden/Furniture/Recycled_Materials/> )";
        String expected2 = "( ?atom = <https://w3id.org/won/example#Atom_01> ) "
                        + "( ?tag = <http://www.dmoz.org/Shopping/Home_and_Garden/Furniture/> )";
        assertThat(actualList, hasItems(expected1, expected2));
        assertEquals("wrong number of results", 2, actualList.size());
    }

    /**
     * This method is for testing the queries. Just rename it to main and execute.
     *
     * @param args
     */
    public static void mainDeactivated(String[] args) {
        loadOntologies();
        System.out.println("executing queries...");
        String queryString = sparqlPreface + "SELECT ?atom ?tag WHERE {?atom rdf:type won:Atom. "
                        + "?atom won:content ?content." + "?content won:tag ?tag." + "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                System.out.println(soln.toString());
            }
        } finally {
            qExec.close();
        }
    }
}