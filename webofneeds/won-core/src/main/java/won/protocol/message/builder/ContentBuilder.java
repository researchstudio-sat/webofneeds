package won.protocol.message.builder;

public class ContentBuilder<PARENT extends BuilderScaffold<PARENT, ?>>
                extends ContentBuilderScaffold<ContentBuilder<PARENT>, PARENT> {
    public ContentBuilder(PARENT parent) {
        super(parent);
    }
}