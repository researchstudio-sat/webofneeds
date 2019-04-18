package won.node.socket;

import java.net.URI;

import won.protocol.model.SocketType;
import won.protocol.vocabulary.WON;

public class GroupSocketConfig extends HardcodedSocketConfig {
    public GroupSocketConfig() {
        super(SocketType.GroupSocket.getURI());
        this.derivationProperties.add(WON.groupMember);
    }

    @Override
    public boolean isConnectionAllowedToType(URI targetSocketType) {
        return SocketType.ChatSocket.getURI().equals(targetSocketType);
    }

    @Override
    public boolean isAutoOpen(URI targetSocketType) {
        return false;
    }
}
