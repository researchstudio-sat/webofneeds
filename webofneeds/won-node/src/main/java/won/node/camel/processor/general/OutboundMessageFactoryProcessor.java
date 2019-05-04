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
package won.node.camel.processor.general;

import java.net.URI;

import won.protocol.message.processor.WonMessageProcessor;

/**
 * WonMessageProcessor that can be used exactly once to generate an outbound
 * message based on a local message.
 */
public abstract class OutboundMessageFactoryProcessor implements WonMessageProcessor {
    private URI messageURI;

    public OutboundMessageFactoryProcessor(URI messageURI) {
        this.messageURI = messageURI;
    }

    public URI getMessageURI() {
        return messageURI;
    }
}
