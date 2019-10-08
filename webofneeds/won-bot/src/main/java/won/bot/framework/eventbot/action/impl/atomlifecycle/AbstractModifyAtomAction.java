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

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;

/**
 * Base class for actions that modifies atoms.
 */
public abstract class AbstractModifyAtomAction extends BaseEventBotAction {
    public AbstractModifyAtomAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    /**
     * Builds a modify message with the given atomURI using the atomDataset as the
     * content wonNodeInformationService and nodeUri will be taken from the
     * eventListenerContext of the Action
     * 
     * @param atomURI uri of the atom that should be replaced
     * @param atomDataset updated content that should be used instead of the
     * existing one
     * @return modify WonMessage
     */
    protected WonMessage buildWonMessage(URI atomURI, Dataset atomDataset) {
        return this.buildWonMessage(getEventListenerContext().getWonNodeInformationService(), atomURI,
                        getEventListenerContext().getNodeURISource().getNodeURI(), atomDataset);
    }

    /**
     * Builds a modify message with the given atomURI using the atomDataset as the
     * content wonNodeInformationService and nodeUri will be taken from the
     * eventListenerContext of the Action
     * 
     * @param wonNodeInformationService
     * @param atomURI uri of the atom that should be replaced
     * @param wonNodeURI
     * @param atomDataset updated content that should be used instead of the
     * existing one
     * @return modify WonMessage
     */
    protected WonMessage buildWonMessage(WonNodeInformationService wonNodeInformationService, URI atomURI,
                    URI wonNodeURI, Dataset atomDataset)
                    throws WonMessageBuilderException {
        RdfUtils.replaceBaseURI(atomDataset, atomURI.toString(), true);
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        return WonMessageBuilder.setMessagePropertiesForReplace(wonNodeInformationService.generateEventURI(wonNodeURI),
                        atomURI, wonNodeURI).addContent(atomModelWrapper.copyDatasetWithoutSysinfo()).build();
    }
}
