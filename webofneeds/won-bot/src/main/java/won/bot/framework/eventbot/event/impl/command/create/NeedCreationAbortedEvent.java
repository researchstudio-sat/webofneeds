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
package won.bot.framework.eventbot.event.impl.command.create;

import java.net.URI;

/**
 * Indicates that need creation was aborted before sending a message to a WoN
 * node.
 */
public class NeedCreationAbortedEvent extends CreateNeedCommandFailureEvent {
    public NeedCreationAbortedEvent(URI needURI, URI needUriBeforeCreation,
                    CreateNeedCommandEvent createNeedCommandEvent, String message) {
        super(needURI, needUriBeforeCreation, createNeedCommandEvent, message);
    }

    public NeedCreationAbortedEvent(URI needURI, URI needUriBeforeCreation,
                    CreateNeedCommandEvent createNeedCommandEvent) {
        super(needURI, needUriBeforeCreation, createNeedCommandEvent);
    }
}
