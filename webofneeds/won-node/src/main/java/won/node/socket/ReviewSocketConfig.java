package won.node.socket;

import java.net.URI;

import won.protocol.model.SocketType;
import won.protocol.vocabulary.WON;

public class ReviewSocketConfig extends HardcodedSocketConfig {
    public ReviewSocketConfig() {
        super(SocketType.ReviewSocket.getURI());
        this.derivationProperties.add(WON.reviews);
    }

    @Override
    public boolean isConnectionAllowedToType(URI targetSocketType) {
        return SocketType.ReviewSocket.getURI().equals(targetSocketType);
    }

    @Override
    public boolean isAutoOpen(URI targetSocketType) {
        return true;
    }
}
