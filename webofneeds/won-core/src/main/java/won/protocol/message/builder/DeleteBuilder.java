package won.protocol.message.builder;

import won.protocol.message.WonMessageType;

public class DeleteBuilder extends AtomSpecificBuilderScaffold<DeleteBuilder> {
    public DeleteBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.type(WonMessageType.DELETE);
    }

    @Override
    public DirectionBuilder<DeleteBuilder> direction() {
        return new DirectionBuilder<DeleteBuilder>(this);
    }

    @Override
    public ContentBuilder<DeleteBuilder> content() {
        return new ContentBuilder<DeleteBuilder>(this);
    }
}
