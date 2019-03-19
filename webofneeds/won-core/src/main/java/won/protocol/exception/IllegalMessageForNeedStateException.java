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

package won.protocol.exception;

import java.net.URI;
import java.text.MessageFormat;

import won.protocol.model.NeedState;

/**
 * User: fkleedorfer Date: 02.11.12
 */
public class IllegalMessageForNeedStateException extends WonProtocolException {
    private String methodName;
    private NeedState needState;
    private URI needURI;

    public IllegalMessageForNeedStateException(final URI needURI, final String methodName, final NeedState needState) {
        super(MessageFormat.format("It is not allowed to call method {0} on need {1}, as it is currently in state {2}.",
                methodName, needURI, needState));
        this.methodName = methodName;
        this.needState = needState;
        this.needURI = needURI;
    }

    public URI getNeedURI() {
        return needURI;
    }

    public String getMethodName() {
        return methodName;
    }

    public NeedState getNeedState() {
        return needState;
    }

}
