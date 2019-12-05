package won.protocol.message.builder;

import java.net.URI;

import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;

public class AtomHintBuilder extends AtomSpecificBuilderScaffold<AtomHintBuilder> {
    public AtomHintBuilder(WonMessageBuilder builder) {
        super(builder);
        builder
                        .type(WonMessageType.ATOM_HINT_MESSAGE)
                        .direction(WonMessageDirection.FROM_EXTERNAL)
                        .timestampNow();
    }

    public AtomHintBuilder hintTargetAtom(URI targetAtomURI) {
        builder.hintTargetAtom(targetAtomURI);
        return this;
    }

    public AtomHintBuilder hintScore(double score) {
        builder.hintScore(score);
        return this;
    }

    public DirectionBuilder<AtomHintBuilder> direction() {
        return new DirectionBuilder<AtomHintBuilder>(this);
    }

    @Override
    public ContentBuilder<AtomHintBuilder> content() {
        return new ContentBuilder<AtomHintBuilder>(this);
    }
}
