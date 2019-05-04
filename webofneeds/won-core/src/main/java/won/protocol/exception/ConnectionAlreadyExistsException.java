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
package won.protocol.exception;

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: fkleedorfer Date: 02.11.12
 */
public class ConnectionAlreadyExistsException extends WonProtocolException {
    private URI connectionURI;
    private URI fromAtomURI;
    private URI toAtomURI;

    public ConnectionAlreadyExistsException(final URI connectionURI, final URI fromAtomURI, final URI toAtomURI) {
        super(MessageFormat.format(
                        "The connection between atoms {0} and {1} that you wanted to connect already exists with this URI: {2}",
                        fromAtomURI.toString(), toAtomURI, connectionURI));
        this.connectionURI = connectionURI;
        this.fromAtomURI = fromAtomURI;
        this.toAtomURI = toAtomURI;
    }

    public URI getFromAtomURI() {
        return fromAtomURI;
    }

    public URI getToAtomURI() {
        return toAtomURI;
    }

    public URI getConnectionURI() {
        return connectionURI;
    }
}
