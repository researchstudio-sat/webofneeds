package won.protocol.message.builder;

import won.protocol.message.WonMessageType;

public class ReplaceBuilder extends AtomSpecificBuilderScaffold<ReplaceBuilder> {
    public ReplaceBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.type(WonMessageType.REPLACE);
    }

    @Override
    public DirectionBuilder<ReplaceBuilder> direction() {
        return new DirectionBuilder<ReplaceBuilder>(this);
    }

    @Override
    public ContentBuilder<ReplaceBuilder> content() {
        return new ContentBuilder<ReplaceBuilder>(this);
    }
}
