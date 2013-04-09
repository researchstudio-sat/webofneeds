/*
 * Copyright 2013  Research Studios Austria Forschungsges.m.b.H.
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

package won.ontology.validator;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * User: fsalcher
 * Date: 03.04.13
 */

public class Validator
{

  private final static String WON_ONTOLOGY_FILE = "/won_ontology_v0.11.rdf";
  private final static String EXAMPLE_ONTOLOGY_FILE = "/won_ontology_example_1.rdf";

  private final static String WON_ONTOLOGY_URI = "http://www.webofneeds.org/model#";
  private final static String EXAMPLE_ONTOLOGY_URI = "http://www.webofneeds.org/example#";


  private static OntModel ontModel;
  private static String sparqlPreface;

  @BeforeClass
  public static void loadOntologies()
  {
    ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);

    System.out.println("loading ontologies... ");
    ontModel.read(Validator.class.getResourceAsStream(WON_ONTOLOGY_FILE), null);
    ontModel.read(Validator.class.getResourceAsStream(EXAMPLE_ONTOLOGY_FILE), null);

    sparqlPreface = "" +
        "PREFIX won: <" + WON_ONTOLOGY_URI + ">" +
        "PREFIX wonexample: <" + EXAMPLE_ONTOLOGY_URI + ">" +
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
        "PREFIX gr: <http://purl.org/goodrelations/v1#>";
  }

  @Test
  public void testCQEvent1()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?event ?time WHERE {?event rdf:type won:Hint. " +
        "?event won:occurredAt ?time}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
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
  public void testCQEvent2()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?event ?eventType WHERE {" +
        "wonexample:EventContainer_01 rdfs:member ?event. " +
        "?event rdf:type won:Event." +
        "?event rdf:type ?eventType." +
        "?eventType rdfs:subClassOf won:Event." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        System.out.println(soln.toString());
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    assertTrue("wrong number of results", actualList.size() >= 1);
  }

  @Test
  public void testCQEvent3()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?event ?originator WHERE {" +
        "wonexample:EventContainer_01 rdfs:member ?event. " +
        "?event rdf:type won:Event." +
        "?event won:hasOriginator ?originator." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.get("?originator").toString());
      }
    } finally {
      qExec.close();
    }
    assertEquals("wrong number of results", 1, actualList.size());
    String expected = "http://www.webofneeds.org/example#Matcher_01";
    assertThat(actualList, hasItems(expected));
  }

  @Test
  public void testCQHow1()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?needModality ?delivery ?price ?currency ?lowerPrice ?upperPrice WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasNeedModality ?needModality." +
        "?needModality gr:availableDeliveryMethods ?delivery." +
        "?needModality won:hasPriceSpecification ?price." +
        "?price ?hasCurrency ?currency." +
        "?price ?hasLowerPriceLimit ?lowerPrice." +
        "?price ?hasUpperPriceLimit ?upperPrice." +
        "}";

    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    // TODO: Extend the expected values to all meaningful results not only one result.
    // TODO: Results should look like the one below but there are some strange results like "?upperPrice = "EUR"^^xsd:string" which has to be investigated!
    String expected = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?delivery = <http://purl.org/goodrelations/v1#DeliveryModePickUp> ) " +
        "( ?price = <http://www.webofneeds.org/example#PriceSpecification_01_1> ) " +
        "( ?currency = \"EUR\"^^xsd:string ) ( ?lowerPrice = \"100.0\"^^xsd:float ) " +
        "( ?upperPrice = \"100.0\"^^xsd:float )";
    assertThat(actualList, hasItems(expected));
  }

  @Test
  public void testCQWhat1()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?text ?description WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasContent ?content." +
        "?content won:textDescription ?text." +
        "?content won:contentDescription ?description." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    assertEquals("wrong number of results", 1, actualList.size());
    String expected = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?text = \"rote Couch mit integrierter Mäusefalle\"^^xsd:string ) " +
        "( ?description = <http://www.webofneeds.org/example#MyCouch> )";
    assertThat(actualList, hasItems(expected));
  }

  @Test
  public void testCQWhat2()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?descType WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasContent ?content." +
        "?content won:contentDescription ?description." +
        "?description rdf:type ?descType." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    String expected = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?descType = <http://www.freebase.com/m/02crq1> )";
    assertThat(actualList, hasItems(expected));
  }

  @Test
  public void testCQWhat4()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?height ?heightV ?heightM ?width ?widthV ?widthM " +
        "?depth ?depthV ?depthM ?weight ?weightV ?weightM WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasContent ?content." +
        "?content won:height ?height." +
        "?height gr:hasValue ?heightV." +
        "?height gr:hasUnitOfMeasurement ?heightM." +
        "?content won:width ?width." +
        "?width gr:hasValue ?widthV." +
        "?width gr:hasUnitOfMeasurement ?widthM." +
        "?content won:depth ?depth." +
        "?depth gr:hasValue ?depthV." +
        "?depth gr:hasUnitOfMeasurement ?depthM." +
        "?content won:weight ?weight." +
        "?weight gr:hasValue ?weightV." +
        "?weight gr:hasUnitOfMeasurement ?weightM." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    assertEquals("wrong number of results", 1, actualList.size());
    String expected = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?height = <http://www.webofneeds.org/example#CouchHeight> ) ( ?heightV = \"50\"^^rdfs:Literal ) " +
        "( ?heightM = \"cm\"^^xsd:string ) ( ?width = <http://www.webofneeds.org/example#CouchWidth> ) " +
        "( ?widthV = \"250\"^^rdfs:Literal ) ( ?widthM = \"cm\"^^xsd:string ) " +
        "( ?depth = <http://www.webofneeds.org/example#CouchDepth> ) ( ?depthV = \"50\"^^rdfs:Literal ) " +
        "( ?depthM = \"cm\"^^xsd:string ) ( ?weight = <http://www.webofneeds.org/example#CouchWeight> ) " +
        "( ?weightV = \"120\"^^rdfs:Literal ) ( ?weightM = \"kg\"^^xsd:string )";
    assertThat(actualList, hasItems(expected));
  }

  @Test
  public void testCQWhat6()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?height ?width ?depth ?weight WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasContent ?content." +
        "?content won:height ?height." +
        "?content won:width ?width." +
        "?content won:depth ?depth." +
        "?content won:weight ?weight." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    assertEquals("wrong number of results", 1, actualList.size());
    String expected = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?height = <http://www.webofneeds.org/example#CouchHeight> ) " +
        "( ?width = <http://www.webofneeds.org/example#CouchWidth> ) " +
        "( ?depth = <http://www.webofneeds.org/example#CouchDepth> ) " +
        "( ?weight = <http://www.webofneeds.org/example#CouchWeight> )";
    assertThat(actualList, hasItems(expected));
  }

  @Test
  public void testCQWhen1()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?creationDate WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:needCreationDate ?creationDate." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    assertEquals("wrong number of results", 2, actualList.size());
    String expected1 = "( ?need = <http://www.webofneeds.org/example#Need_02> ) " +
        "( ?creationDate = \"2013-03-20T14:29:00+02:00\"^^xsd:dateTimeStamp )";
    String expected2 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?creationDate = \"2013-04-04T07:26:00+02:00\"^^xsd:dateTimeStamp )";
    assertThat(actualList, hasItems(expected1, expected2));
  }

  @Test
  public void testCQWhen3()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?needModality ?timeConstraint ?p ?o WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasNeedModality ?needModality." +
        "?needModality won:availableAtTime ?timeConstraint." +
        "?timeConstraint ?p ?o." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    String expected1 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_1_2> ) " +
        "( ?p = <http://www.webofneeds.org/model#recurIn> ) ( ?o = \"P7D\"^^xsd:duration )";
    String expected2 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_1_2> ) " +
        "( ?p = <http://www.webofneeds.org/model#endTime> ) ( ?o = \"2013-04-05T17:00:00+02:00\"^^xsd:dateTimeStamp )";
    String expected3 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_1_2> ) " +
        "( ?p = <http://www.webofneeds.org/model#startTime> ) ( ?o = \"2013-04-05T09:00:00+02:00\"^^xsd:dateTimeStamp )";
    String expected4 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_1_2> ) " +
        "( ?p = <http://www.webofneeds.org/model#recurTimes> ) ( ?o = 0 )";
    String expected5 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_1_1> ) " +
        "( ?p = <http://www.webofneeds.org/model#recurIn> ) ( ?o = \"P7D\"^^xsd:duration )";
    String expected6 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_1_1> ) " +
        "( ?p = <http://www.webofneeds.org/model#endTime> ) ( ?o = \"2013-04-04T17:00:00+02:00\"^^xsd:dateTimeStamp )";
    String expected7 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_1_1> ) " +
        "( ?p = <http://www.webofneeds.org/model#startTime> ) ( ?o = \"2013-04-04T09:00:00+02:00\"^^xsd:dateTimeStamp )";
    String expected8 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_1_1> ) " +
        "( ?p = <http://www.webofneeds.org/model#recurTimes> ) ( ?o = 0 )";
    String expected9 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_2> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_2> ) " +
        "( ?p = <http://www.webofneeds.org/model#endTime> ) ( ?o = \"2013-05-04T09:00:00+02:00\"^^xsd:dateTimeStamp )";
    String expected10 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_2> ) " +
        "( ?timeConstraint = <http://www.webofneeds.org/example#TimeConstraint_01_2> ) " +
        "( ?p = <http://www.webofneeds.org/model#startTime> ) ( ?o = \"2013-04-04T09:00:00+02:00\"^^xsd:dateTimeStamp )";
    assertThat(actualList, hasItems(expected1, expected2, expected3, expected4, expected5, expected6, expected7,
        expected8, expected9, expected10));
  }

  @Test
  public void testCQWhere1()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?needModality ?location ?p ?o WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasNeedModality ?needModality." +
        "?needModality won:availableAtLocation ?location." +
        "?location ?p ?o." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    String expected1 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?location = <http://www.webofneeds.org/example#Thurngasse_8> ) " +
        "( ?p = <http://www.w3.org/2003/01/geo/wgs84_pos#latitude> ) " +
        "( ?o = \"48.218746\"^^xsd:float )";
    String expected2 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_1> ) " +
        "( ?location = <http://www.webofneeds.org/example#Thurngasse_8> ) " +
        "( ?p = <http://www.w3.org/2003/01/geo/wgs84_pos#longitude> ) " +
        "( ?o = \"16.360283\"^^xsd:float )";
    String expected3 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?needModality = <http://www.webofneeds.org/example#NeedModality_01_2> ) " +
        "( ?location = <http://www.webofneeds.org/example#Österreich> ) " +
        "( ?p = <http://www.webofneeds.org/model#hasISOCode> ) ( ?o = \"AT\"^^xsd:string )";
    assertThat(actualList, hasItems(expected1, expected2, expected3));
  }

  @Test
  public void testCQWhere2()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?location ?concealed WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasNeedModality ?modality." +
        "?modality won:availableAtLocation ?location." +
        "?location won:isConcealed ?concealed." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    assertEquals("wrong number of results", 2, actualList.size());
    String expected1 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?location = <http://www.webofneeds.org/example#Thurngasse_8> ) ( ?concealed = true )";
    String expected2 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?location = <http://www.webofneeds.org/example#Österreich> ) ( ?concealed = false )";
    assertThat(actualList, hasItems(expected1, expected2));
  }

  @Test
  public void testCQWho1()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?owner WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasOwner ?owner." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    assertEquals("wrong number of results", 1, actualList.size());
    String expected = "( ?need = <http://www.webofneeds.org/example#Need_01> ) " +
        "( ?owner = <http://www.webofneeds.org/example#NeedOwner_1> )";
    assertThat(actualList, hasItems(expected));
  }

  @Test
  public void testCQState1()
  {
    System.out.println("executing queries...");
    String queryString = sparqlPreface +
        "SELECT ?need ?active WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:isActive ?active." +
        "}";
    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    List<String> actualList = new ArrayList<String>();
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }
    } finally {
      qExec.close();
    }
    String expected1 = "( ?need = <http://www.webofneeds.org/example#Need_02> ) ( ?active = true )";
    String expected2 = "( ?need = <http://www.webofneeds.org/example#Need_01> ) ( ?active = true )";
    assertThat(actualList, hasItems(expected1, expected2));
    assertEquals("wrong number of results", 2, actualList.size());
  }

  public static void main(String[] args)
  {
    loadOntologies();
    System.out.println("executing queries...");

//    String queryString = sparqlPreface +
//        "SELECT ?event ?eventType WHERE {" +
//        "wonexample:EventContainer_01 rdfs:member ?event. " +
//        "?event rdf:type won:Event." +
//        "?event rdf:type ?eventType." +
//        "?eventType rdf:type won:Event." +
//        "}";

//    String queryString = sparqlPreface +
//        "SELECT ?eventType WHERE {" +
//        "?eventType rdfs:subClassOf won:Event. " +
//        "}";

    String queryString = sparqlPreface +
        "SELECT ?need ?needModality ?delivery ?price ?currency ?lowerPrice ?upperPrice WHERE {" +
        "?need rdf:type won:Need. " +
        "?need won:hasNeedModality ?needModality." +
        "?needModality gr:availableDeliveryMethods ?delivery." +
        "?needModality won:hasPriceSpecification ?price." +
        "?price ?hasCurrency ?currency." +
        "?price ?hasLowerPriceLimit ?lowerPrice." +
        "?price ?hasUpperPriceLimit ?upperPrice." +
        "}";

    Query query = QueryFactory.create(queryString);
    QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
    try {
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        System.out.println(soln.toString());
      }
    } finally {
      qExec.close();
    }
  }
}

