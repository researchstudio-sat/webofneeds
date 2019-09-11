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
package won.protocol.rest;

import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: fkleedorfer Date: 28.11.12
 */
public class RDFMediaType {
    public static final MediaType TEXT_TURTLE = new MediaType("text", "turtle");
    public static final MediaType APPLICATION_RDF_XML = new MediaType("application", "rdf+xml");
    public static final MediaType APPLICATION_X_TURTLE = new MediaType("application", "x-turtle");
    public static final MediaType TEXT_RDF_N3 = new MediaType("text", "rdf+n3");
    public static final MediaType APPLICATION_JSON = new MediaType("application", "json");
    public static final MediaType APPLICATION_TRIG = new MediaType("application", "trig");
    public static final MediaType APPLICATION_JSONLD = new MediaType("application", "ld+json");
    public static final MediaType APPLICATION_NQUADS = new MediaType("application", "n-quads");
    public static final Set<MediaType> rdfMediaTypes;
    static {
        HashSet<MediaType> types = new HashSet<MediaType>(Arrays
                        .asList(TEXT_TURTLE, APPLICATION_RDF_XML, APPLICATION_X_TURTLE, TEXT_RDF_N3,
                                APPLICATION_JSON, APPLICATION_TRIG, APPLICATION_JSONLD, APPLICATION_NQUADS));
        rdfMediaTypes = Collections.unmodifiableSet(types);
    }

    public static boolean isRDFMediaType(MediaType mediaType) {
        return rdfMediaTypes.contains(mediaType);
    }
}
