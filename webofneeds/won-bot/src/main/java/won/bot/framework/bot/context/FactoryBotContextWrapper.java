/*
 * Copyright 2017 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.framework.bot.context;

import java.net.URI;

public class FactoryBotContextWrapper extends BotContextWrapper {
    private final String factoryListName = getBotName() + ":factoryList";
    private final String factoryInternalIdName = getBotName() + ":factoryInternalId";
    private final String factoryOfferToFactoryAtomMapName = getBotName() + ":factoryOfferToFactoryAtomMap";

    public FactoryBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }

    public String getFactoryListName() {
        return factoryListName;
    }

    public boolean isFactoryAtom(URI uri) {
        return getBotContext().isInNamedAtomUriList(uri, factoryListName);
    }

    public URI getURIFromInternal(URI uri) {
        return (URI) getBotContext().loadFromObjectMap(factoryInternalIdName, uri.toString());
    }

    public void addInternalIdToUriReference(URI internalUri, URI uri) {
        getBotContext().saveToObjectMap(factoryInternalIdName, internalUri.toString(), uri);
    }

    public URI getFactoryAtomURIFromOffer(URI offerURI) {
        return (URI) getBotContext().loadFromObjectMap(factoryOfferToFactoryAtomMapName, offerURI.toString());
    }

    public void addFactoryAtomURIOfferRelation(URI offerURI, URI factoryAtomURI) {
        getBotContext().saveToObjectMap(factoryOfferToFactoryAtomMapName, offerURI.toString(), factoryAtomURI);
    }
}
