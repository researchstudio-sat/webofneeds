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

package won.protocol.model;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * User: fkleedorfer
 * Date: 20.11.12
 */
public class WON {
    //TODO change URI style to '#' style for the ontology
    public static final String BASE_URI = "http://purl.org/webofneeds/model#";

    private static Model m = ModelFactory.createDefaultModel();

    //NEW
    public static final Property IS_IN_STATE = m.createProperty(BASE_URI + "isInState");
    public static final Property HAS_OWNER = m.createProperty(BASE_URI + "hasOwner");
    public static final Property HAS_CONNECTIONS = m.createProperty(BASE_URI + "hasConnections");
    public static final Property HAS_CONTENT = m.createProperty(BASE_URI + "hasContent");
    public static final Property NEED_CONTENT = m.createProperty(BASE_URI + "NeedContent");
    public static final Property TEXT_DESCRIPTION = m.createProperty(BASE_URI + "textDescription");

    public static final Property NEED_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI + "needProtocolEndpoint");
    public static final Property MATCHER_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI + "matcherProtocolEndpoint");
    public static final Property OWNER_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI + "ownerProtocolEndpoint");

    //OLD
    @Deprecated
    public static final Property OWNER = m.createProperty(BASE_URI + "Owner");
    @Deprecated
    public static final Property NEED_DESCRIPTION = m.createProperty(BASE_URI + "NeedDescription");
    @Deprecated
    public static final Property STATE = m.createProperty(BASE_URI + "state");
    @Deprecated
    public static final Property RESOURCE_URI = m.createProperty(BASE_URI + "resourceUri");

    public static final Property REMOTE_CONNECTION = m.createProperty(BASE_URI + "remoteConnection");
    public static final Property REMOTE_NEED = m.createProperty(BASE_URI + "remoteNeed");
    public static final Property BELONGS_TO_NEED = m.createProperty(BASE_URI + "belongsToNeed");

    /**
     * Returns the URI for this schema.
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }

}
