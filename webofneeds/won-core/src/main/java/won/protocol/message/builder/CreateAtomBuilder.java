package won.protocol.message.builder;

import won.protocol.message.WonMessageType;

public class CreateAtomBuilder extends AtomSpecificBuilderScaffold<CreateAtomBuilder> {
    public CreateAtomBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.type(WonMessageType.CREATE_ATOM);
    }

    @Override
    public DirectionBuilder<CreateAtomBuilder> direction() {
        return new DirectionBuilder<CreateAtomBuilder>(this);
    }

    @Override
    public ContentBuilder<CreateAtomBuilder> content() {
        return new ContentBuilder<CreateAtomBuilder>(this);
    }
}
