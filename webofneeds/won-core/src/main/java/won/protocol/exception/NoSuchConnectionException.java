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
public class NoSuchConnectionException extends WonProtocolException {
    private URI unknownConnectionURI;
    private URI unknownSocketURI;
    private URI unknownTargetSocketURI;

    public NoSuchConnectionException(String message) {
        super(message);
    }

    public URI getUnknownConnectionURI() {
        return unknownConnectionURI;
    }

    public URI getUnknownSocketURI() {
        return unknownSocketURI;
    }

    public URI getUnknownTargetSocketURI() {
        return unknownTargetSocketURI;
    }

    public NoSuchConnectionException(final URI unknownConnectionURI) {
        super(MessageFormat.format("No connection with the URI {0} is known on this server.", unknownConnectionURI));
        this.unknownConnectionURI = unknownConnectionURI;
    }

    public NoSuchConnectionException(final URI unknownSocketURI, final URI unknownTargetSocketURI) {
        super(MessageFormat.format("No connection between socket {0} and targetSocket {1} is known on this server.",
                        unknownSocketURI, unknownTargetSocketURI));
        this.unknownSocketURI = unknownSocketURI;
        this.unknownTargetSocketURI = unknownTargetSocketURI;
    }

    public NoSuchConnectionException(final Long unknownConnectionID) {
        super(MessageFormat.format("No connection with the URI {0} is known on this server.", unknownConnectionID));
        this.unknownConnectionURI = unknownConnectionURI;
    }
}
