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
package won.bot.framework.eventbot.event.impl.command.replace;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseAtomSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandSuccessEvent;

/**
 * Indicates that creating an atom succeeded.
 */
public class ReplaceCommandSuccessEvent extends BaseAtomSpecificEvent
                implements MessageCommandSuccessEvent, ReplaceCommandResultEvent {
    private ReplaceCommandEvent replaceCommandEvent;
    private String message;

    public ReplaceCommandSuccessEvent(URI atomURI, ReplaceCommandEvent replaceCommandEvent, String message) {
        super(atomURI);
        this.replaceCommandEvent = replaceCommandEvent;
        this.message = message;
    }

    public ReplaceCommandSuccessEvent(URI atomURI, ReplaceCommandEvent createAtomCommandEvent) {
        super(atomURI);
        this.replaceCommandEvent = createAtomCommandEvent;
    }

    @Override
    public MessageCommandEvent getOriginalCommandEvent() {
        return replaceCommandEvent;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }
}
