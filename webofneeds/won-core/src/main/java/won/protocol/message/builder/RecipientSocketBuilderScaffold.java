package won.protocol.message.builder;

import java.net.URI;

abstract class RecipientSocketBuilderScaffold<THIS extends RecipientSocketBuilderScaffold<THIS, PARENT>, PARENT extends BuilderScaffold<PARENT, ?>>
                extends BuilderScaffold<THIS, PARENT> {
    public RecipientSocketBuilderScaffold(PARENT parent) {
        super(parent);
    }

    /**
     * Sets the recipientSocket.
     * 
     * @param recipientSocket
     * @return the parent builder
     */
    public abstract PARENT recipient(URI recipientSocket);

    /**
     * Skips setting the recipientSocket.
     * 
     * @return the parent builder
     */
    public abstract PARENT noRecipient(URI recipientSocket);
}