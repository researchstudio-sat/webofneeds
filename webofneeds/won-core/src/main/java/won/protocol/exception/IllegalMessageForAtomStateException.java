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

import won.protocol.model.AtomState;

/**
 * User: fkleedorfer Date: 02.11.12
 */
public class IllegalMessageForAtomStateException extends WonProtocolException {
    private String methodName;
    private AtomState atomState;
    private URI atomURI;

    public IllegalMessageForAtomStateException(final URI atomURI, final String methodName, final AtomState atomState) {
        super(MessageFormat.format("It is not allowed to call method {0} on atom {1}, as it is currently in state {2}.",
                        methodName, atomURI, atomState));
        this.methodName = methodName;
        this.atomState = atomState;
        this.atomURI = atomURI;
    }

    public URI getAtomURI() {
        return atomURI;
    }

    public String getMethodName() {
        return methodName;
    }

    public AtomState getAtomState() {
        return atomState;
    }
}
