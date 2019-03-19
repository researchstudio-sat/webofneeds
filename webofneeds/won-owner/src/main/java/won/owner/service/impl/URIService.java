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

package won.owner.service.impl;

import java.net.URI;

/**
 * User: fkleedorfer Date: 12.02.13
 */
public class URIService {
    private URI ownerProtocolOwnerServiceEndpointURI;
    private URI defaultOwnerProtocolNeedServiceEndpointURI;
    private URI ownerProtocolOwnerURI;

    public URI getOwnerProtocolOwnerServiceEndpointURI() {
        return ownerProtocolOwnerServiceEndpointURI;
    }

    public void setOwnerProtocolOwnerServiceEndpointURI(final URI ownerProtocolOwnerServiceEndpointURI) {
        this.ownerProtocolOwnerServiceEndpointURI = ownerProtocolOwnerServiceEndpointURI;
    }

    public URI getDefaultOwnerProtocolNeedServiceEndpointURI() {
        return defaultOwnerProtocolNeedServiceEndpointURI;
    }

    public void setDefaultOwnerProtocolNeedServiceEndpointURI(final URI defaultOwnerProtocolNeedServiceEndpointURI) {
        this.defaultOwnerProtocolNeedServiceEndpointURI = defaultOwnerProtocolNeedServiceEndpointURI;
    }

    public URI getOwnerProtocolOwnerURI() {
        return ownerProtocolOwnerURI;
    }

    public void setOwnerProtocolOwnerURI(URI ownerProtocolOwnerURI) {
        this.ownerProtocolOwnerURI = ownerProtocolOwnerURI;
    }
}
