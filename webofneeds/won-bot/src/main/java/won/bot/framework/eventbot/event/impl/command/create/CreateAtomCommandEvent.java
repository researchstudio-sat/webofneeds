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
import won.protocol.vocabulary.WXCHAT;

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
    @Deprecated
    private List<URI> sockets;
    // the name of the atom uri list to save the atom uri to
    @Deprecated
    private String uriListName = BotContext.DEFAULT_ATOM_LIST_NAME;
    // sets the UsedForTesting flag
    @Deprecated
    private boolean usedForTesting;
    // sets the do not match flag
    @Deprecated
    private boolean doNotMatch;

    /**
     * @deprecated flags and sockets should be set directly in the atomDataset,
     * uriListName should not be set within an Event anymore
     */
    @Deprecated
    public CreateAtomCommandEvent(Dataset atomDataset, String uriListName, boolean usedForTesting, boolean doNotMatch,
                    URI... sockets) {
        this.atomDataset = atomDataset;
        if (uriListName != null) {
            this.uriListName = uriListName;
        }
        if (sockets != null && sockets.length > 0) {
            this.sockets = Arrays.asList(sockets);
        } else {
            this.sockets = Collections.singletonList(WXCHAT.ChatSocket.getUri());
        }
        this.usedForTesting = usedForTesting;
        this.doNotMatch = doNotMatch;
    }

    /**
     * @deprecated sockets should be set directly in the atomDataset, uriListName
     * should not be set within an Event anymore
     */
    @Deprecated
    public CreateAtomCommandEvent(Dataset atomDataset, String uriListName, URI... sockets) {
        this(atomDataset, uriListName, false, false, sockets);
    }

    public CreateAtomCommandEvent(Dataset atomDataset) {
        this(atomDataset, null, null);
    }

    /**
     * @deprecated sockets should be set directly in the atomDataset
     */
    @Deprecated
    public CreateAtomCommandEvent(Dataset atomDataset, URI... sockets) {
        this(atomDataset, null, sockets);
    }

    /**
     * @deprecated uriListName should not be set within an Event anymore
     */
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

    @Deprecated
    public String getUriListName() {
        return uriListName;
    }

    @Deprecated
    public List<URI> getSockets() {
        return sockets;
    }

    @Deprecated
    public boolean isUsedForTesting() {
        return usedForTesting;
    }

    @Deprecated
    public boolean isDoNotMatch() {
        return doNotMatch;
    }
}
