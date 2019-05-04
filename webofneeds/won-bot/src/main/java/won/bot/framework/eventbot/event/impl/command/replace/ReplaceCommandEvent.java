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

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;

/**
 * Instructs the bot to replace an atom's content
 */
public class ReplaceCommandEvent implements MessageCommandEvent {
    // the content to set instead of the current content
    private Dataset atomDataset;

    public ReplaceCommandEvent(Dataset atomDataset) {
        this.atomDataset = atomDataset;
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.REPLACE;
    }

    public Dataset getAtomDataset() {
        return atomDataset;
    }
}
