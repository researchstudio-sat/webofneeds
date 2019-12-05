package won.protocol.message.builder;

import won.protocol.message.WonMessageType;

/**
 * Prepares the WonMessagebuilder with direction FROM_OWNER, type CONNECT and
 * the current timestamp as timestamp.
 * 
 * @author fkleedorfer
 */
public class ConnectBuilder extends ConnectionSpecificBuilderScaffold<ConnectBuilder> {
    public ConnectBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.type(WonMessageType.CONNECT);
    }

    @Override
    public DirectionBuilder<ConnectBuilder> direction() {
        return new DirectionBuilder<ConnectBuilder>(this);
    }

    @Override
    public ContentBuilder<ConnectBuilder> content() {
        return new ContentBuilder<ConnectBuilder>(this);
    }

    @Override
    public SenderSocketBuilder<ConnectBuilder> sockets() {
        return new SenderSocketBuilder<ConnectBuilder>(this);
    }
}