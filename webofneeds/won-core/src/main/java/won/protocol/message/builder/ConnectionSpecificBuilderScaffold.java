package won.protocol.message.builder;

import won.protocol.message.WonMessageDirection;

abstract class ConnectionSpecificBuilderScaffold<THIS extends ConnectionSpecificBuilderScaffold<THIS>>
                extends TerminalBuilderBase<THIS> {
    public ConnectionSpecificBuilderScaffold(WonMessageBuilder builder) {
        super(builder);
    }

    /**
     * Sets the {@link WonMessageDirection.FROM_SYSTEM} direction.
     * 
     * @return the parent builder
     */
    public abstract DirectionBuilderScaffold<?, THIS> direction();

    public abstract ContentBuilderScaffold<?, THIS> content();

    public abstract SenderSocketBuilderScaffold<?, THIS> sockets();
}