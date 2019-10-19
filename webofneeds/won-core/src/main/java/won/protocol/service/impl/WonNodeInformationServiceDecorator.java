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
package won.protocol.service.impl;

import java.net.URI;
import java.util.Optional;

import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;

/**
 * Base class for decorators of the WonNodeInformationService..
 */
public class WonNodeInformationServiceDecorator implements WonNodeInformationService {
    private WonNodeInformationService delegate;

    public WonNodeInformationServiceDecorator(WonNodeInformationService delegate) {
        this.delegate = delegate;
    }

    public WonNodeInformationService getDelegate() {
        return delegate;
    }

    @Override
    public WonNodeInfo getWonNodeInformation(URI wonNodeURI) {
        return delegate.getWonNodeInformation(wonNodeURI);
    }

    @Override
    public URI generateEventURI() {
        return delegate.generateEventURI();
    }

    @Override
    public URI generateEventURI(URI wonNodeURI) {
        return delegate.generateEventURI(wonNodeURI);
    }

    @Override
    public URI generateConnectionURI() {
        return delegate.generateConnectionURI();
    }

    @Override
    public URI generateConnectionURI(URI wonNodeURI) {
        return delegate.generateConnectionURI(wonNodeURI);
    }

    @Override
    public URI generateAtomURI() {
        return delegate.generateAtomURI();
    }

    @Override
    public URI generateAtomURI(URI wonNodeURI) {
        return delegate.generateAtomURI(wonNodeURI);
    }

    @Override
    public URI getDefaultWonNodeURI() {
        return delegate.getDefaultWonNodeURI();
    }

    @Override
    public URI getWonNodeUri(URI resourceURI) {
        return delegate.getWonNodeUri(resourceURI);
    }

    @Override
    public boolean isValidEventURI(URI eventURI) {
        return delegate.isValidAtomURI(eventURI);
    }

    @Override
    public boolean isValidEventURI(URI eventURI, URI wonNodeURI) {
        return delegate.isValidEventURI(eventURI, wonNodeURI);
    }

    @Override
    public boolean isValidConnectionURI(URI connectionURI) {
        return delegate.isValidConnectionURI(connectionURI);
    }

    @Override
    public boolean isValidConnectionURI(URI connectionURI, URI wonNodeURI) {
        return delegate.isValidConnectionURI(connectionURI, wonNodeURI);
    }

    @Override
    public boolean isValidAtomURI(URI atomURI) {
        return delegate.isValidAtomURI(atomURI);
    }

    @Override
    public boolean isValidAtomURI(URI atomURI, URI wonNodeURI) {
        return delegate.isValidAtomURI(atomURI, wonNodeURI);
    }

    @Override
    public Optional<WonNodeInfo> getWonNodeInformationForURI(URI someURI, Optional<URI> requesterWebID) {
        return delegate.getWonNodeInformationForURI(someURI, requesterWebID);
    }

    @Override
    public WonNodeInfo getDefaultWonNodeInfo() {
        return delegate.getDefaultWonNodeInfo();
    }
}
