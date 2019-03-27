/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary for named graphs, see http://www.w3.org/2004/03/trix/
 */
public class RDFG {
    public static final String BASE_URI = "http://www.w3.org/2004/03/trix/rdfg-1/";
    public static final String DEFAULT_PREFIX = "rdfg";
    private static Model m = ModelFactory.createDefaultModel();
    public static final Resource GRAPH = m.createProperty(BASE_URI + "Graph");
    public static final Property SUBGRAPH_OF = m.createProperty(BASE_URI + "subGraphOf");
    public static final Property EQUIVALENT_GRAPH = m.createProperty(BASE_URI + "equivalentGraph");

    /**
     * returns the URI for this schema
     * 
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
