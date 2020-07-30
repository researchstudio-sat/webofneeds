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
package won.bot.framework.eventbot.action.impl.atomlifecycle;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WONMATCH;
import won.protocol.vocabulary.WXCHAT;

/**
 * Base class for actions that create atoms.
 */
public abstract class AbstractCreateAtomAction extends BaseEventBotAction {
    @Deprecated
    protected List<URI> sockets;
    @Deprecated
    protected String uriListName;
    // indicates if the won:DoNotMatch flag is to be set
    @Deprecated
    protected boolean usedForTesting;
    @Deprecated
    protected boolean doNotMatch;

    public AbstractCreateAtomAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
        this.sockets = Collections.emptyList();
        this.doNotMatch = false;
        this.usedForTesting = false;
        this.uriListName = eventListenerContext.getBotContextWrapper().getAtomCreateListName();
    }

    /**
     * Creates an atom with the specified sockets. If no socket is specified, the
     * chatSocket will be used, Flag 'UsedForTesting' will be set. uriListName is
     * used from the set botContextWrapper getAtomCreateListName
     *
     * @deprecated sockets should be set within the dataset of the atom
     */
    @Deprecated
    public AbstractCreateAtomAction(EventListenerContext eventListenerContext, URI... sockets) {
        this(eventListenerContext, eventListenerContext.getBotContextWrapper().getAtomCreateListName(), sockets);
    }

    /**
     * Creates an atom with the specified sockets. If no socket is specified, the
     * chatSocket will be used, Flag 'UsedForTesting' will be set.
     * 
     * @deprecated sockets should be set within the dataset of the atom
     */
    @Deprecated
    public AbstractCreateAtomAction(EventListenerContext eventListenerContext, String uriListName, URI... sockets) {
        this(eventListenerContext, uriListName, true, false, sockets);
    }

    /**
     * Creates an atom with the specified sockets. If no socket is specified, the
     * chatSocket will be used.
     * 
     * @deprecated sockets, and flags should be set within the dataset of the atom
     */
    @Deprecated
    public AbstractCreateAtomAction(EventListenerContext eventListenerContext, String uriListName,
                    final boolean usedForTesting, final boolean doNotMatch, URI... sockets) {
        super(eventListenerContext);
        if (sockets == null || sockets.length == 0) {
            // add the default socket if none is present.
            this.sockets = new ArrayList<>(1);
            this.sockets.add(WXCHAT.ChatSocket.getUri());
        } else {
            this.sockets = Arrays.asList(sockets);
        }
        this.doNotMatch = doNotMatch;
        this.usedForTesting = usedForTesting;
        this.uriListName = uriListName;
    }

    protected WonMessage createWonMessage(URI atomURI, Dataset atomDataset) throws WonMessageBuilderException {
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        RdfUtils.replaceBaseURI(atomDataset, atomURI.toString(), true);
        return WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atomModelWrapper.copyDatasetWithoutSysinfo())
                        .build();
    }

    /**
     * @deprecated flags should be set within the dataset of the atom
     */
    @Deprecated
    protected WonMessage createWonMessage(URI atomURI, Dataset atomDataset, final boolean usedForTesting,
                    final boolean doNotMatch) {
        return createWonMessage(atomURI, null, atomDataset, usedForTesting, doNotMatch);
    }

    /**
     * @deprecated wonNodeUri parameter is obsolete
     */
    @Deprecated
    protected WonMessage createWonMessage(URI atomURI, URI wonNodeURI,
                    Dataset atomDataset) throws WonMessageBuilderException {
        return createWonMessage(atomURI, null, atomDataset, usedForTesting, doNotMatch);
    }

    /**
     * @deprecated wonNodeUri parameter is obsolete, flags should be set within
     * atomDataset
     */
    @Deprecated
    protected WonMessage createWonMessage(URI atomURI, URI wonNodeURI,
                    Dataset atomDataset, final boolean usedForTesting, final boolean doNotMatch)
                    throws WonMessageBuilderException {
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        if (doNotMatch) {
            atomModelWrapper.addFlag(WONMATCH.NoHintForMe);
            atomModelWrapper.addFlag(WONMATCH.NoHintForCounterpart);
        }
        if (usedForTesting) {
            atomModelWrapper.addFlag(WONMATCH.UsedForTesting);
        }
        RdfUtils.replaceBaseURI(atomDataset, atomURI.toString(), true);
        return WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content().dataset(atomModelWrapper.copyDatasetWithoutSysinfo())
                        .build();
    }

    /**
     * @deprecated flags should be set within atomDataset
     */
    @Deprecated
    public void setUsedForTesting(final boolean usedForTesting) {
        this.usedForTesting = usedForTesting;
    }

    /**
     * @deprecated flags should be set within atomDataset
     */
    @Deprecated
    public void setDoNotMatch(final boolean doNotMatch) {
        this.doNotMatch = doNotMatch;
    }
}
