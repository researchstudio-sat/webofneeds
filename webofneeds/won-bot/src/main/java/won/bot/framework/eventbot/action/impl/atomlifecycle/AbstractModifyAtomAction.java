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
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

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
     * @param modifiedAtomDataset updated content that should be used instead of the
     * existing one
     * @return modify WonMessage
     */
    protected final WonMessage buildWonMessage(URI atomURI, Dataset modifiedAtomDataset) {
        Dataset originalAtomDataset = getEventListenerContext().getLinkedDataSource().getDataForResource(atomURI);
        if (originalAtomDataset == null) {
            throw new IllegalStateException("Cannot modify atom " + atomURI + " : retrieved dataset is null");
        }
        URI wonNodeUri = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(originalAtomDataset, atomURI);
        URI eventUri = getEventListenerContext().getWonNodeInformationService().generateEventURI(wonNodeUri);
        RdfUtils.replaceBaseURI(modifiedAtomDataset, atomURI.toString(), true);
        AtomModelWrapper modifiedAtomModelWrapper = new AtomModelWrapper(modifiedAtomDataset);
        return WonMessageBuilder
                        .replace(eventUri)
                        .atom(atomURI)
                        .content().dataset(modifiedAtomModelWrapper.copyDatasetWithoutSysinfo())
                        .build();
    }
}
