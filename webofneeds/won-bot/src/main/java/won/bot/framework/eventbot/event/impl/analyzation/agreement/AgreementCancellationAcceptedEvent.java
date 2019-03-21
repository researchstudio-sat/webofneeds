package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import won.protocol.model.Connection;

import java.net.URI;

/**
 * Created by fsuda on 27.11.2017.
 */
public class AgreementCancellationAcceptedEvent extends AgreementEvent {
  public AgreementCancellationAcceptedEvent(Connection con, URI agreementUri) {
    super(con, agreementUri);
  }

  public AgreementCancellationAcceptedEvent(Connection con, URI agreementUri, URI messageUri) {
    super(con, agreementUri);
  }
}
