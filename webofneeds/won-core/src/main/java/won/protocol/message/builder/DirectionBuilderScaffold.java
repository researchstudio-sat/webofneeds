package won.protocol.message.builder;

import won.protocol.message.WonMessageDirection;

abstract class DirectionBuilderScaffold<THIS extends DirectionBuilderScaffold<THIS, PARENT>, PARENT extends BuilderScaffold<PARENT, ?>>
                extends BuilderScaffold<THIS, PARENT> {
    public DirectionBuilderScaffold(PARENT parent) {
        super(parent);
    }

    public DirectionBuilderScaffold(WonMessageBuilder builder) {
        super(builder);
    }

    /**
     * Sets the {@link WonMessageDirection.FROM_SYSTEM} direction.
     * 
     * @return the parent builder
     */
    public PARENT fromSystem() {
        builder.direction(WonMessageDirection.FROM_SYSTEM);
        return parent.get();
    }

    /**
     * Sets the {@link WonMessageDirection.FROM_OWNER} direction.
     * 
     * @return the parent builder
     */
    public PARENT fromOwner() {
        builder.direction(WonMessageDirection.FROM_OWNER);
        return parent.get();
    }

    /**
     * Sets the {@link WonMessageDirection.FROM_EXTERNAL} direction.
     * 
     * @return the parent builder
     */
    public PARENT fromExternal() {
        builder.direction(WonMessageDirection.FROM_EXTERNAL);
        return parent.get();
    }
}