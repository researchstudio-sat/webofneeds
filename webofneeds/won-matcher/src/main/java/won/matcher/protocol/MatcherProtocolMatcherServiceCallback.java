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
package won.matcher.protocol;

import java.net.URI;

import org.apache.jena.query.Dataset;

/**
 * User: fkleedorfer Date: 21.01.14
 */
public interface MatcherProtocolMatcherServiceCallback {
    void onRegistered(URI wonNodeUri);

    void onNewNeed(final URI wonNodeURI, URI needURI, Dataset content);

    void onNeedActivated(final URI wonNodeURI, URI needURI);

    void onNeedDeactivated(final URI wonNodeURI, URI needURI);
}
