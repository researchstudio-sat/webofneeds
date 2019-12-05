package won.protocol.message.builder;

public class ModificationBuilder<PARENT extends BuilderScaffold<PARENT, ?>>
                extends ModificationBuilderScaffold<ModificationBuilder<PARENT>, PARENT> {
    public ModificationBuilder(PARENT parent) {
        super(parent);
    }
}
