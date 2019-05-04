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
package won.protocol.service;

import java.net.URI;

import org.apache.jena.rdf.model.Model;

import won.protocol.message.WonMessage;

/**
 * Interface defining methods for atom communication. TODO [REFACTORING]: delete
 * this and move methods to node interface
 */
public interface MatcherFacingAtomCommunicationService {
    /**
     * Notifies the atom of a matching otherAtom with the specified match score.
     * Originator identifies the entity making the call. Normally, originator is a
     * matching service. Expects <> won:socket [SOCKET] in the RDF content, will
     * choose a socket supported by the atom if none is present.
     *
     * @param atomURI the URI of the atom
     * @param otherAtom URI of the other atom (may be on the local atomserver)
     * @param score match score between 0.0 (bad) and 1.0 (good). Implementations
     * treat lower values as 0.0 and higher values as 1.0.
     * @param originator an URI identifying the calling entity
     * @param content (optional) an optional RDF graph containing more detailed
     * information about the hint. The null releative URI ('<>') inside that graph,
     * as well as the base URI of the graph will be attached to the resource
     * identifying the match event.
     * @throws won.protocol.exception.NoSuchAtomException if atomURI is not a known
     * atom URI
     * @throws won.protocol.exception.IllegalMessageForAtomStateException if the
     * atom is not active
     */
    public void hint(URI atomURI, URI otherAtom, double score, URI originator, Model content, WonMessage wonMessage)
                    throws Exception;
}
