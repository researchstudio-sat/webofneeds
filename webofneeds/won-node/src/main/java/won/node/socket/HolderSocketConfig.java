package won.node.socket;

import java.net.URI;

import won.protocol.model.SocketType;
import won.protocol.vocabulary.WON;

public class HolderSocketConfig extends HardcodedSocketConfig {
    public HolderSocketConfig() {
        super(SocketType.HolderSocket.getURI());
        this.derivationProperties.add(WON.holds);
    }

    @Override
    public boolean isConnectionAllowedToType(URI targetSocketType) {
        return SocketType.HoldableSocket.getURI().equals(targetSocketType);
    }

    @Override
    public boolean isAutoOpen(URI targetSocketType) {
        return false;
    }
}
