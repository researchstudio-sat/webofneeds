package won.protocol.message.builder;

import won.protocol.message.WonMessageType;

public class CloseBuilder extends ConnectionSpecificBuilderScaffold<CloseBuilder> {
    public CloseBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.type(WonMessageType.CLOSE);
    }

    @Override
    public DirectionBuilder<CloseBuilder> direction() {
        return new DirectionBuilder<CloseBuilder>(this);
    }

    @Override
    public ContentBuilder<CloseBuilder> content() {
        return new ContentBuilder<CloseBuilder>(this);
    }

    @Override
    public SenderSocketBuilder<CloseBuilder> sockets() {
        return new SenderSocketBuilder<CloseBuilder>(this);
    }
}