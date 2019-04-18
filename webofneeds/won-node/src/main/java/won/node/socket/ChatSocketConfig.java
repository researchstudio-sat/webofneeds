package won.node.socket;

import java.net.URI;

import won.protocol.model.SocketType;
import won.protocol.vocabulary.WON;

public class ChatSocketConfig extends HardcodedSocketConfig {
    public ChatSocketConfig() {
        super(SocketType.ChatSocket.getURI());
        this.derivationProperties.add(WON.connectedWith);
    }

    /**
     * For now, we treat the chat socket as the default socket that is itself
     * allowed to connect to all.
     */
    @Override
    public boolean isConnectionAllowedToType(URI targetSocketType) {
        return true;
    }

    @Override
    public boolean isAutoOpen(URI targetSocketType) {
        return false;
    }
}
