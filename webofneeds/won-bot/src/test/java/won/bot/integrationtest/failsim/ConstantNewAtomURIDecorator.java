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
package won.bot.integrationtest.failsim;

import java.net.URI;

import won.bot.framework.eventbot.EventListenerContext;
import won.protocol.service.WonNodeInformationService;
import won.protocol.service.impl.WonNodeInformationServiceDecorator;

/**
 * Decorates the EventListenerContext such that AtomURIs generated through the
 * WonNodeInformationService are always the same.
 */
public class ConstantNewAtomURIDecorator extends BaseEventListenerContextDecorator {
    public ConstantNewAtomURIDecorator(EventListenerContext delegate, String atomURISuffix) {
        super(delegate);
        this.atomURISuffix = atomURISuffix;
    }

    private String atomURISuffix;

    @Override
    public WonNodeInformationService getWonNodeInformationService() {
        WonNodeInformationService delegate = super.getWonNodeInformationService();
        return new WonNodeInformationServiceDecorator(delegate) {
            @Override
            public URI generateAtomURI() {
                return URI.create(getDelegate().getWonNodeInformation(getDefaultWonNodeURI()).getAtomURIPrefix() + "/"
                                + atomURISuffix);
            }

            @Override
            public URI generateAtomURI(URI wonNodeURI) {
                return URI.create(getDelegate().getWonNodeInformation(wonNodeURI).getAtomURIPrefix() + "/"
                                + atomURISuffix);
            }
        };
    }
}
