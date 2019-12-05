package won.protocol.message.builder;

import java.net.URI;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

public class HintFeedbackMessageBuilder extends BuilderBase {
    private Optional<Boolean> feedbackValue = Optional.empty();

    public HintFeedbackMessageBuilder(WonMessageBuilder builder) {
        super(builder);
        builder.timestampNow()
                        .direction(WonMessageDirection.FROM_OWNER)
                        .type(WonMessageType.HINT_FEEDBACK_MESSAGE);
    }

    public WonMessage build() {
        if (builder.connectionURI == null) {
            throw new IllegalStateException("Cannot call build() before connection(URI)");
        }
        if (!feedbackValue.isPresent()) {
            throw new IllegalStateException("Cannot call build() before good() or bad()");
        }
        Model contentModel = WonRdfUtils.MessageUtils.binaryFeedbackMessage(builder.connectionURI, feedbackValue.get());
        Resource msgResource = contentModel.createResource(builder.getMessageURI().toString());
        RdfUtils.replaceBaseResource(contentModel, msgResource);
        return builder
                        .content(contentModel)
                        .build();
    }

    public HintFeedbackMessageBuilder good() {
        this.feedbackValue = Optional.of(true);
        return this;
    }

    public HintFeedbackMessageBuilder bad() {
        this.feedbackValue = Optional.of(false);
        return this;
    }

    public HintFeedbackMessageBuilder connection(URI connectionURI) {
        builder.connection(connectionURI);
        return this;
    }

    public HintFeedbackMessageBuilder binaryFeedback(boolean isGood) {
        if (isGood) {
            return this.good();
        } else {
            return this.bad();
        }
    }
}
