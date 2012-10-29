import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 29.10.12
 * Time: 13:14
 * To change this template use File | Settings | File Templates.
 */
public class WONStorageProviderRegistry extends OpenStorageProviderRegistry {
    public WONStorageProviderRegistry(WONUserAuthorization auth, MemoryRosterManager roster) {
        add(auth);
        add(roster);

        // provider from external modules, low coupling, fail when modules are not present
        add("org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider");
        add("org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeInMemoryStorageProvider");
        //add("org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.MemoryOfflineStorageProvider");
    }
}
