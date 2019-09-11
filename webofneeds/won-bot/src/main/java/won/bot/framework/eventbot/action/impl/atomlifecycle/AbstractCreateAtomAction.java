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
import java.util.List;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.SocketType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WONMATCH;

/**
 * Base class for actions that create atoms.
 */
public abstract class AbstractCreateAtomAction extends BaseEventBotAction {
    protected List<URI> sockets;
    protected String uriListName;
    // indicates if the won:DoNotMatch flag is to be set
    protected boolean usedForTesting;
    protected boolean doNotMatch;

    /**
     * Creates an atom with the specified sockets. If no socket is specified, the
     * chatSocket will be used, Flag 'UsedForTesting' will be set. uriListName is
     * used from the set botcontextwrapper getAtomCreateListName
     */
    public AbstractCreateAtomAction(EventListenerContext eventListenerContext, URI... sockets) {
        this(eventListenerContext, eventListenerContext.getBotContextWrapper().getAtomCreateListName(), sockets);
    }

    /**
     * Creates an atom with the specified sockets. If no socket is specified, the
     * chatSocket will be used, Flag 'UsedForTesting' will be set.
     */
    public AbstractCreateAtomAction(EventListenerContext eventListenerContext, String uriListName, URI... sockets) {
        this(eventListenerContext, uriListName, true, false, sockets);
    }

    /**
     * Creates an atom with the specified sockets. If no socket is specified, the
     * chatSocket will be used.
     */
    public AbstractCreateAtomAction(EventListenerContext eventListenerContext, String uriListName,
                    final boolean usedForTesting, final boolean doNotMatch, URI... sockets) {
        super(eventListenerContext);
        if (sockets == null || sockets.length == 0) {
            // add the default socket if none is present.
            this.sockets = new ArrayList<URI>(1);
            this.sockets.add(SocketType.ChatSocket.getURI());
        } else {
            this.sockets = Arrays.asList(sockets);
        }
        this.doNotMatch = doNotMatch;
        this.usedForTesting = usedForTesting;
        this.uriListName = uriListName;
    }

    protected WonMessage createWonMessage(URI atomURI, Dataset atomDataset) throws WonMessageBuilderException {
        return createWonMessage(atomURI, atomDataset, false, false);
    }

    protected WonMessage createWonMessage(URI atomURI, Dataset atomDataset, final boolean usedForTesting,
                    final boolean doNotMatch) {
        return createWonMessage(getEventListenerContext().getWonNodeInformationService(), atomURI,
                        getEventListenerContext().getNodeURISource().getNodeURI(), atomDataset, usedForTesting,
                        doNotMatch);
    }

    protected WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI atomURI,
                    URI wonNodeURI, Dataset atomDataset) throws WonMessageBuilderException {
        return createWonMessage(wonNodeInformationService, atomURI, wonNodeURI, atomDataset, usedForTesting,
                        doNotMatch);
    }

    protected WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI atomURI,
                    URI wonNodeURI, Dataset atomDataset, final boolean usedForTesting, final boolean doNotMatch)
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
        return WonMessageBuilder.setMessagePropertiesForCreate(wonNodeInformationService.generateEventURI(wonNodeURI),
                        atomURI, wonNodeURI).addContent(atomModelWrapper.copyDataset()).build();
    }

    public void setUsedForTesting(final boolean usedForTesting) {
        this.usedForTesting = usedForTesting;
    }

    public void setDoNotMatch(final boolean doNotMatch) {
        this.doNotMatch = doNotMatch;
    }
    // private boolean socket(SocketType socketToCheck) {
    // for (URI socket : sockets) {
    // if (socket.equals(socketToCheck.getURI())) {
    // return true;
    // }
    // }
    // return false;
    // }
}
