package won.protocol.message.builder;

import java.net.URI;
import java.util.Collection;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;

/**
 * Builds a message that notifies connected atoms of a change to an atom.
 * 
 * @author fkleedorfer
 */
public class ChangeNotificationBuilder
                extends ConnectionSpecificBuilderScaffold<ChangeNotificationBuilder> {
    public ChangeNotificationBuilder(WonMessageBuilder builder) {
        super(builder);
        builder
                        .type(WonMessageType.CHANGE_NOTIFICATION)
                        .direction(WonMessageDirection.FROM_SYSTEM);
    }

    /**
     * Allows to change the direction from the default
     * {@link WonMessageDirection.FROM_SYSTEM}.
     */
    @Override
    public DirectionBuilder<ChangeNotificationBuilder> direction() {
        return new DirectionBuilder<ChangeNotificationBuilder>(this);
    }

    @Override
    public ContentBuilder<ChangeNotificationBuilder> content() {
        return new ContentBuilder<ChangeNotificationBuilder>(this);
    }

    @Override
    public SenderSocketBuilder<ChangeNotificationBuilder> sockets() {
        return new SenderSocketBuilder<ChangeNotificationBuilder>(this);
    }

    /**
     * Forwards the specified message as part of the message being built.
     * 
     * @param toForward
     * @return the connection message builder.
     */
    public ChangeNotificationBuilder forward(WonMessage toForward) {
        builder.forward(toForward);
        return this;
    }

    /**
     * Sets the specified URIs as 'injection targets' using
     * msg:injectIntoConnection.
     * 
     * @param injectionTargets
     * @return
     */
    public ChangeNotificationBuilder injectIntoConnections(Collection<URI> injectionTargets) {
        builder.injectIntoConnections(injectionTargets);
        return this;
    }
}