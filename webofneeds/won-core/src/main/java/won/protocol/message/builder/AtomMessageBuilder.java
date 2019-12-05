package won.protocol.message.builder;

import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;

/**
 * Sets message properties for sending a 'atom message' from System to Owner,
 * i.e. a notification from the node to the owner. This message will have no
 * effect on atom or connection states and it is expected that a payload (e.g.
 * via setTextMessage()) is added to the message builder prior to calling the
 * build() method.
 */
public class AtomMessageBuilder extends AtomSpecificBuilderScaffold<AtomMessageBuilder> {
    public AtomMessageBuilder(WonMessageBuilder builder) {
        super(builder);
        builder
                        .type(WonMessageType.ATOM_MESSAGE)
                        .direction(WonMessageDirection.FROM_SYSTEM);
    }

    @Override
    public DirectionBuilder<AtomMessageBuilder> direction() {
        return new DirectionBuilder<AtomMessageBuilder>(this);
    }

    @Override
    public ContentBuilder<AtomMessageBuilder> content() {
        return new ContentBuilder<AtomMessageBuilder>(this);
    }
}
