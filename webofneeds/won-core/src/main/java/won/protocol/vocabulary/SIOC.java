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
 * WGS84 Geo Positioning vocabulary. User: Alan Tus Date: 21.04.13. Time: 22:13
 */
public class SIOC {
    public static final String BASE_URI = "http://rdfs.org/sioc/ns#";
    public static final String DEFAULT_PREFIX = "sioc";
    private static Model m = ModelFactory.createDefaultModel();
    public static final Resource POST = m.createResource(BASE_URI + "Post");
    public static final Property HAS_REPLY = m.createProperty(BASE_URI, "hasReply");

    /**
     * returns the URI for this schema
     * 
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
