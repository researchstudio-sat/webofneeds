package won.node.socket;

import java.net.URI;

import won.protocol.model.SocketType;
import won.protocol.vocabulary.WON;

public class HoldableSocketConfig extends HardcodedSocketConfig {
    public HoldableSocketConfig() {
        super(SocketType.HoldableSocket.getURI());
        this.derivationProperties.add(WON.heldBy);
    }

    @Override
    public boolean isConnectionAllowedToType(URI targetSocketType) {
        return SocketType.HolderSocket.getURI().equals(targetSocketType);
    }

    @Override
    public boolean isAutoOpen(URI targetSocketType) {
        return false;
    }
}
