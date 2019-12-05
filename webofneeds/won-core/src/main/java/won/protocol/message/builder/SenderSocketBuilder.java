package won.protocol.message.builder;

import java.net.URI;

import won.protocol.message.WonMessage;

public class SenderSocketBuilder<PARENT extends BuilderScaffold<PARENT, ?>>
                extends SenderSocketBuilderScaffold<SenderSocketBuilder<PARENT>, PARENT> {
    SenderSocketBuilder(PARENT parent) {
        super(parent);
    }

    /**
     * Assumes that the specified message is a message that we received in a
     * connection and we want to send a message back. The senderSocket of the
     * specified message is used as the recipientSocket of the one we are building,
     * and vice versa.
     * 
     * @param connectMessage
     * @return
     */
    public PARENT reactingTo(WonMessage connectMessage) {
        if (!connectMessage.getMessageTypeRequired().isConnectionSpecificMessage()) {
            throw new IllegalStateException(
                            "Can only perform reactingTo(msg) with a connection specific message, but the one that was provided is not: "
                                            + connectMessage.toShortStringForDebug());
        }
        builder.senderSocket(connectMessage.getRecipientSocketURIRequired());
        builder.recipientSocket(connectMessage.getSenderSocketURIRequired());
        return parent.get();
    }

    public RecipientSocketBuilder<PARENT> sender(URI senderSocket) {
        builder.senderSocket(senderSocket);
        return new RecipientSocketBuilder<PARENT>(parent.get());
    }

    @Override
    public RecipientSocketBuilder<PARENT> noSender(URI recipientSocket) {
        return new RecipientSocketBuilder<PARENT>(parent.get());
    }
}