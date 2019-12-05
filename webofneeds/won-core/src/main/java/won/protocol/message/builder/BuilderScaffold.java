package won.protocol.message.builder;

import java.util.Optional;

import won.protocol.message.WonMessageDirection;

/**
 * Base class for builders that need sub-builders for content, sockets, etc.
 * <p>
 * The type parameters <code><THIS></code> and <code><PARENT></code> are used as
 * the return types of builder methods. The resulting rather contrived
 * inheritance structure allows for sub-builders to return instances of specific
 * parent builders they were instantiated for, instead of the base class of all
 * builders. Inspired by this article:
 * </p>
 * https://www.sitepoint.com/self-types-with-javas-generics/
 * 
 * @author fkleedorfer
 * @param <THIS>
 * @param <PARENT>
 */
abstract class BuilderScaffold<THIS extends BuilderScaffold<THIS, PARENT>, PARENT extends BuilderScaffold<PARENT, ?>> {
    protected Optional<PARENT> parent = Optional.empty();
    protected WonMessageBuilder builder;

    public BuilderScaffold(PARENT parent) {
        this.parent = Optional.of(parent);
        this.builder = parent.builder;
    }

    public BuilderScaffold(WonMessageBuilder builder) {
        this.builder = builder;
        builder
                        .timestampNow()
                        .direction(WonMessageDirection.FROM_OWNER);
    }
}