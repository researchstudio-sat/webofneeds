/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.exception;

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: LEIH-NB
 * Date: 13.12.13
 */
public class EndpointConfigurationFailedException extends WonProtocolException {
    private URI needURI;
    public EndpointConfigurationFailedException(final URI needURI)
    {
        super(MessageFormat.format("setting up endpoint for need URI {0} failed",needURI.toString()));
        this.needURI = needURI;
    }
}
