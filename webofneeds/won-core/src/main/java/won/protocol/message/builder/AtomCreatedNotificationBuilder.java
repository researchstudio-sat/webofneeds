package won.protocol.message.builder;

import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;

public class AtomCreatedNotificationBuilder
                extends AtomSpecificBuilderScaffold<AtomCreatedNotificationBuilder> {
    public AtomCreatedNotificationBuilder(WonMessageBuilder builder) {
        super(builder);
        builder
                        .type(WonMessageType.ATOM_CREATED_NOTIFICATION)
                        .direction(WonMessageDirection.FROM_SYSTEM);
    }

    @Override
    public DirectionBuilder<AtomCreatedNotificationBuilder> direction() {
        return new DirectionBuilder<AtomCreatedNotificationBuilder>(this);
    }

    @Override
    public ContentBuilder<AtomCreatedNotificationBuilder> content() {
        return new ContentBuilder<AtomCreatedNotificationBuilder>(this);
    }
}
