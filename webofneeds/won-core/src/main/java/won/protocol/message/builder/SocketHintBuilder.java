package won.protocol.message.builder;

import java.net.URI;

import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;

public class SocketHintBuilder extends TerminalBuilderBase<SocketHintBuilder> {
    public SocketHintBuilder(WonMessageBuilder builder) {
        super(builder);
        builder
                        .type(WonMessageType.SOCKET_HINT_MESSAGE)
                        .direction(WonMessageDirection.FROM_EXTERNAL)
                        .timestampNow();
    }

    public SocketHintBuilder recipientSocket(URI recipientSocketURI) {
        builder.recipientSocket(recipientSocketURI);
        return this;
    }

    public SocketHintBuilder hintTargetSocket(URI targetSocketURI) {
        builder.hintTargetSocket(targetSocketURI);
        return this;
    }

    public SocketHintBuilder hintScore(double score) {
        builder.hintScore(score);
        return this;
    }

    public DirectionBuilder<SocketHintBuilder> direction() {
        return new DirectionBuilder<SocketHintBuilder>(this);
    }
}
