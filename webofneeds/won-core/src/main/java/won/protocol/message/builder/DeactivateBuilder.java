package won.protocol.message.builder;

import won.protocol.message.WonMessageType;

public class DeactivateBuilder extends AtomSpecificBuilderScaffold<DeactivateBuilder> {
    public DeactivateBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.type(WonMessageType.DEACTIVATE);
    }

    @Override
    public DirectionBuilder<DeactivateBuilder> direction() {
        return new DirectionBuilder<DeactivateBuilder>(this);
    }

    @Override
    public ContentBuilder<DeactivateBuilder> content() {
        return new ContentBuilder<DeactivateBuilder>(this);
    }
}
