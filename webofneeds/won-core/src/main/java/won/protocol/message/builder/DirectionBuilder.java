package won.protocol.message.builder;

public class DirectionBuilder<PARENT extends BuilderScaffold<PARENT, ?>>
                extends DirectionBuilderScaffold<DirectionBuilder<PARENT>, PARENT> {
    public DirectionBuilder(PARENT parent) {
        super(parent);
    }
}