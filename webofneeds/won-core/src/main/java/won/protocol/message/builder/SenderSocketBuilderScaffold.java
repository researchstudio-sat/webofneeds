package won.protocol.message.builder;

import java.net.URI;

abstract class SenderSocketBuilderScaffold<THIS extends SenderSocketBuilderScaffold<THIS, PARENT>, PARENT extends BuilderScaffold<PARENT, ?>>
                extends BuilderScaffold<THIS, PARENT> {
    public SenderSocketBuilderScaffold(PARENT parent) {
        super(parent);
    }

    /**
     * Sets the senderSocket.
     * 
     * @param senderSocket
     * @return the builder for setting the recipient socket.
     */
    public abstract RecipientSocketBuilderScaffold<?, PARENT> sender(URI senderSocket);

    /**
     * Skips setting the senderSocket.
     * 
     * @return the builder for setting the recipient socket.
     */
    public abstract RecipientSocketBuilderScaffold<?, PARENT> noSender(URI recipientSocket);
}