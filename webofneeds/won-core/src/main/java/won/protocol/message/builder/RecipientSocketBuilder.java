package won.protocol.message.builder;

import java.net.URI;

public class RecipientSocketBuilder<PARENT extends BuilderScaffold<PARENT, ?>>
                extends RecipientSocketBuilderScaffold<RecipientSocketBuilder<PARENT>, PARENT> {
    public RecipientSocketBuilder(PARENT parent) {
        super(parent);
    }

    @Override
    public PARENT recipient(URI recipientSocket) {
        builder.recipientSocket(recipientSocket);
        return parent.get();
    }

    @Override
    public PARENT noRecipient(URI recipientSocket) {
        return parent.get();
    }
}