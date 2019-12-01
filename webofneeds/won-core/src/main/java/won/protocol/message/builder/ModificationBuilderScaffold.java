package won.protocol.message.builder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import won.protocol.message.WonMessage;
import won.protocol.vocabulary.WONMOD;

public class ModificationBuilderScaffold<THIS extends ModificationBuilderScaffold<THIS, PARENT>, PARENT extends BuilderScaffold<PARENT, ?>>
                extends BuilderScaffold<THIS, PARENT> {
    public ModificationBuilderScaffold(PARENT parent) {
        super(parent);
    }

    /**
     * Retract the specified messages as identified by their URIs.
     * 
     * @param other the message URIs to retract
     * @return the parent builder
     */
    public PARENT retracts(URI... other) {
        builder.addToMessageResource(WONMOD.retracts, other);
        return parent.get();
    }

    /**
     * Retract the specified messages.
     * 
     * @param other the messages to retract
     * @return the parent builder
     */
    public PARENT retracts(WonMessage... other) {
        builder.addToMessageResource(WONMOD.retracts, mapMessageURIs(other));
        return parent.get();
    }

    List<URI> mapMessageURIs(WonMessage... other) {
        return Arrays.asList(other).stream().map(m -> m.getMessageURIRequired()).collect(Collectors.toList());
    }
}
