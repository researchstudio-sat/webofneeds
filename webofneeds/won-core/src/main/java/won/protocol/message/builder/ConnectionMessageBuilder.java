package won.protocol.message.builder;

import java.net.URI;
import java.util.Collection;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;

public class ConnectionMessageBuilder
                extends ConnectionSpecificBuilderScaffold<ConnectionMessageBuilder>
                implements AgreementSpecificBuilderScaffold<ConnectionMessageBuilder>,
                ModificationSpecificBuilderScaffold<ConnectionMessageBuilder> {
    public ConnectionMessageBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.type(WonMessageType.CONNECTION_MESSAGE);
    }

    @Override
    public DirectionBuilder<ConnectionMessageBuilder> direction() {
        return new DirectionBuilder<ConnectionMessageBuilder>(this);
    }

    @Override
    public ContentBuilder<ConnectionMessageBuilder> content() {
        return new ContentBuilder<ConnectionMessageBuilder>(this);
    }

    @Override
    public SenderSocketBuilder<ConnectionMessageBuilder> sockets() {
        return new SenderSocketBuilder<ConnectionMessageBuilder>(this);
    }

    /**
     * Forwards the specified message as part of the message being built.
     * 
     * @param toForward
     * @return the connection message builder.
     */
    public ConnectionMessageBuilder forward(WonMessage toForward) {
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
    public ConnectionMessageBuilder injectIntoConnections(Collection<URI> injectionTargets) {
        builder.injectIntoConnections(injectionTargets);
        return this;
    }

    @Override
    public AgreementBuilder<ConnectionMessageBuilder> agreement() {
        return new AgreementBuilder<ConnectionMessageBuilder>(this);
    }

    @Override
    public ModificationBuilder<ConnectionMessageBuilder> modification() {
        return new ModificationBuilder<ConnectionMessageBuilder>(this);
    }
}