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
package won.bot.framework.eventbot.filter.impl;

import java.net.URI;

import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.ResponseEvent;
import won.bot.framework.eventbot.filter.EventFilter;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;

/**
 * Accepts only ResponseEvents that are remote responses for the specified
 * message
 */
public class RemoteResponseEventFilter implements EventFilter {
    private URI responseSenderAtomURI;
    private URI originalMessageURI;

    public RemoteResponseEventFilter(final URI responseSenderAtomURI, final URI originalMessageURI) {
        this.responseSenderAtomURI = responseSenderAtomURI;
        this.originalMessageURI = originalMessageURI;
    }

    public static RemoteResponseEventFilter forWonMessage(WonMessage wonMessage) {
        if (!wonMessage.getMessageTypeRequired().causesOutgoingMessage()) {
            throw new IllegalArgumentException(
                            "Cannot build RemoteResponseFilter: Message " + wonMessage.getMessageURI()
                                            + " does not cause a remote response");
        }
        return new RemoteResponseEventFilter(
                        WonMessageUtils.getRecipientAtomURIRequired(wonMessage),
                        wonMessage.getMessageURI());
    }

    @Override
    public boolean accept(final Event event) {
        if (event instanceof ResponseEvent) {
            URI messageBeingRespondedTo = ((ResponseEvent) event).getOriginalMessageURI();
            URI senderOfResponse = ((ResponseEvent) event).getSenderAtomURI();
            return (this.originalMessageURI.equals(messageBeingRespondedTo)
                            && this.responseSenderAtomURI.equals(senderOfResponse));
        }
        return false;
    }
}
