/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.xmpp;

import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xmpp.modules.roster.persistence.AbstractRosterManager;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 29.10.12
 * Time: 13:14
 * To change this template use File | Settings | File Templates.
 */
public class WONStorageProviderRegistry extends OpenStorageProviderRegistry {
    public WONStorageProviderRegistry(WONUserAuthentication auth, AbstractRosterManager roster) {
        add(auth);
        add(roster);

        // provider from external modules, low coupling, fail when modules are not present
        add("org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider");
        add("org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeInMemoryStorageProvider");
        //add("org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.MemoryOfflineStorageProvider");
    }
}

