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
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * User: fsalcher
 * Date: 03.04.13
 */
public class Validator {

    public static void main(String[] args) {


        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);

        System.out.println("loading ontologies...");
        ontModel.read(Validator.class.getResourceAsStream("/won_ontology_v0.11.rdf"), null);
        ontModel.read(Validator.class.getResourceAsStream("/won_ontology_example_1.rdf"), null);

        System.out.println("executing queries...");
//        String queryString = "SELECT ?s ?p ?o WHERE {<http://www.webofneeds.org/example#Hint_01_1> ?p ?o}" ;
        String queryString =
                "PREFIX won: <http://www.webofneeds.org/model#>" +
                        "PREFIX wonexample: <http://www.webofneeds.org/example#>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "SELECT ?event ?time WHERE {?event rdf:type won:Hint. " +
                        "?event won:occurredAt ?time}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontModel);
        try {
            ResultSet results = qExec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution();
                System.out.println(soln.toString());
                RDFNode x = soln.get("?s");       // Get a result variable by name.
                RDFNode r = soln.get("?p"); // Get a result variable - must be a resource
                RDFNode l = soln.get("?o");   // Get a result variable - must be a literal

//                System.out.println("s: " + x.toString() + ", p: " + r.toString() + ", o: " + l.toString());
            }
        } finally {
            qExec.close();
        }


    }


}
