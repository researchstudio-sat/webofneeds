package won.protocol.message.builder;

import won.protocol.message.WonMessageType;

public class ActivateBuilder extends AtomSpecificBuilderScaffold<ActivateBuilder> {
    public ActivateBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.type(WonMessageType.ACTIVATE);
    }

    @Override
    public DirectionBuilder<ActivateBuilder> direction() {
        return new DirectionBuilder<ActivateBuilder>(this);
    }

    @Override
    public ContentBuilder<ActivateBuilder> content() {
        return new ContentBuilder<ActivateBuilder>(this);
    }
}
