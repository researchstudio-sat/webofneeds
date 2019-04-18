package won.protocol.model.unread;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import won.protocol.model.ConnectionState;

public class UnreadMessageInfoForAtom {
    private URI atomURI;
    private UnreadMessageInfo unreadMessageInfo;
    private Map<ConnectionState, UnreadMessageInfo> unreadInfoByConnectionState;
    private Collection<UnreadMessageInfoForConnection> unreadMessageInfoForConnections;

    public UnreadMessageInfoForAtom(URI atomURI) {
        super();
        this.atomURI = atomURI;
        this.unreadInfoByConnectionState = new HashMap<>();
        this.unreadMessageInfoForConnections = new ArrayList<>();
    }

    public void addUnreadMessageInfoForConnection(UnreadMessageInfoForConnection connectionInfo) {
        this.unreadMessageInfoForConnections.add(connectionInfo);
        this.unreadMessageInfo = aggregate(connectionInfo.getUnreadInformation(), this.unreadMessageInfo);
        aggregateByConnectionState(connectionInfo);
    }

    private void aggregateByConnectionState(UnreadMessageInfoForConnection connectionInfo) {
        UnreadMessageInfo info = this.unreadInfoByConnectionState.get(connectionInfo.getConnectionState());
        info = aggregate(connectionInfo.getUnreadInformation(), info);
        this.unreadInfoByConnectionState.put(connectionInfo.getConnectionState(), info);
    }

    private UnreadMessageInfo aggregate(UnreadMessageInfo newInfo, UnreadMessageInfo aggregatedInfo) {
        if (aggregatedInfo == null) {
            return newInfo.clone();
        }
        return aggregatedInfo.aggregateWith(newInfo);
    }

    public URI getAtomURI() {
        return atomURI;
    }

    public Map<ConnectionState, UnreadMessageInfo> getUnreadInfoByConnectionState() {
        return unreadInfoByConnectionState;
    }

    public Collection<UnreadMessageInfoForConnection> getUnreadMessageInfoForConnections() {
        return unreadMessageInfoForConnections;
    }
}
