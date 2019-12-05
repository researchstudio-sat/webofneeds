package won.protocol.message.builder;

import won.protocol.message.WonMessage;

/**
 * Base class for builders that do not need sub-builders for content, sockets,
 * etc.
 * 
 * @author fkleedorfer
 */
public abstract class BuilderBase {
    protected WonMessageBuilder builder;

    public BuilderBase(WonMessageBuilder builder) {
        this.builder = builder;
    }

    /**
     * Builds the {@link WonMessage} object.
     * 
     * @return
     */
    public WonMessage build() {
        return builder.build();
    }
}