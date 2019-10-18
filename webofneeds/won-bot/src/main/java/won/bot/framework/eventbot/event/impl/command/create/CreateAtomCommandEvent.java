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

import org.apache.jena.query.Dataset;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.model.SocketType;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Instructs the bot to create an atom.
 */
public class CreateAtomCommandEvent implements MessageCommandEvent {
    // the model of the atom's content
    private Dataset atomDataset;
    // the sockets the new atom should have
    private List<URI> sockets;
    // the name of the atom uri list to save the atom uri to
    private String uriListName = BotContext.DEFAULT_ATOM_LIST_NAME;
    // sets the UsedForTesting flag
    private boolean usedForTesting;
    // sets the do not match flag
    private boolean doNotMatch;

    public CreateAtomCommandEvent(Dataset atomDataset, String uriListName, boolean usedForTesting, boolean doNotMatch,
                    URI... sockets) {
        this.atomDataset = atomDataset;
        if (this.uriListName != null) {
            this.uriListName = uriListName;
        }
        if (sockets != null && sockets.length > 0) {
            this.sockets = Arrays.asList(sockets);
        } else {
            this.sockets = Collections.singletonList(SocketType.ChatSocket.getURI());
        }
        this.usedForTesting = usedForTesting;
        this.doNotMatch = doNotMatch;
    }

    public CreateAtomCommandEvent(Dataset atomDataset, String uriListName, URI... sockets) {
        this(atomDataset, uriListName, false, false, sockets);
    }

    public CreateAtomCommandEvent(Dataset atomDataset) {
        this(atomDataset, null, null);
    }

    public CreateAtomCommandEvent(Dataset atomDataset, URI... sockets) {
        this(atomDataset, null, sockets);
    }

    public CreateAtomCommandEvent(Dataset atomDataset, String uriListName) {
        this(atomDataset, uriListName, null);
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CREATE_ATOM;
    }

    public Dataset getAtomDataset() {
        return atomDataset;
    }

    public String getUriListName() {
        return uriListName;
    }

    public List<URI> getSockets() {
        return sockets;
    }

    public boolean isUsedForTesting() {
        return usedForTesting;
    }

    public boolean isDoNotMatch() {
        return doNotMatch;
    }
}
