package won.protocol.message.builder;

import java.net.URI;

import won.protocol.message.WonMessageDirection;

abstract class AtomSpecificBuilderScaffold<THIS extends AtomSpecificBuilderScaffold<THIS>>
                extends TerminalBuilderBase<THIS> {
    public AtomSpecificBuilderScaffold(WonMessageBuilder builder) {
        super(builder);
    }

    /**
     * Sets the rdf:type of the message to a {@link WonMessageDirection}. Default is
     * {@link WonMessageDirection.FROM_OWNER}.
     * 
     * @return the parent builder
     */
    public abstract DirectionBuilderScaffold<?, THIS> direction();

    /**
     * Allows for setting content.
     * 
     * @return
     */
    public abstract ContentBuilderScaffold<?, THIS> content();

    /**
     * Set the msg:atom property.
     * 
     * @param atomURI
     * @return
     */
    @SuppressWarnings("unchecked")
    public THIS atom(URI atomURI) {
        builder.atom(atomURI);
        return (THIS) this;
    }
}