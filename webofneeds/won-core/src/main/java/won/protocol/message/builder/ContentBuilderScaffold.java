package won.protocol.message.builder;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

abstract class ContentBuilderScaffold<THIS extends ContentBuilderScaffold<THIS, PARENT>, PARENT extends BuilderScaffold<PARENT, ?>>
                extends BuilderScaffold<THIS, PARENT> {
    public ContentBuilderScaffold(PARENT parent) {
        super(parent);
    }

    /**
     * Sets the specified model as the content of the message. Will overwrite any
     * other content previously set.
     * 
     * @param content
     * @return the parent builder
     */
    public PARENT model(Model content) {
        builder.content(content);
        return parent.get();
    }

    /**
     * Uses the specified text to build a model that is added as content. Will
     * overwrite any other content previously set. If the text is null or empty,
     * this call has no effect.
     * 
     * @param textMessage
     * @return the parent builder
     */
    public PARENT text(String text) {
        if (text != null && text.trim().length() > 0) {
            builder.textMessage(text);
        }
        return parent.get();
    }

    public PARENT dataset(Dataset content) {
        builder.content(content);
        return parent.get();
    }
}