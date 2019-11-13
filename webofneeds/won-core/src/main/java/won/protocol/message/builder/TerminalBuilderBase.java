package won.protocol.message.builder;

import won.protocol.message.WonMessage;

/**
 * Base class for builders that support the build() method.
 * 
 * @author fkleedorfer
 */
public abstract class TerminalBuilderBase<THIS extends TerminalBuilderBase<THIS>>
                extends BuilderScaffold<THIS, THIS> {
    public TerminalBuilderBase(WonMessageBuilder builder) {
        super(builder);
    }

    public WonMessage build() {
        return builder.build();
    }
}
