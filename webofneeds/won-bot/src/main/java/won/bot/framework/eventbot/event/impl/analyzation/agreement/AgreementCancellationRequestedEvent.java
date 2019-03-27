package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import java.net.URI;

import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public class AgreementCancellationRequestedEvent extends AgreementEvent {
    // Uri of the message that contains the agreementUri as proposeToCancel
    // if null then this event is regarding an already Accepted Cancellation
    private URI messageUri;

    public AgreementCancellationRequestedEvent(Connection con, URI agreementUri) {
        super(con, agreementUri);
    }

    public AgreementCancellationRequestedEvent(Connection con, URI agreementUri, URI messageUri) {
        super(con, agreementUri);
        this.messageUri = messageUri;
    }

    public URI getMessageUri() {
        return messageUri;
    }
}
