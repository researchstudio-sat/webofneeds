package won.protocol.message.builder;

public class AgreementBuilder<PARENT extends BuilderScaffold<PARENT, ?>>
                extends AgreementBuilderScaffold<AgreementBuilder<PARENT>, PARENT> {
    public AgreementBuilder(PARENT parent) {
        super(parent);
    }
}
