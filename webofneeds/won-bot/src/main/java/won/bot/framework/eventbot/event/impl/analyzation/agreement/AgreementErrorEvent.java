package won.bot.framework.eventbot.event.impl.analyzation.agreement;

import won.protocol.model.Connection;

import java.net.URI;

/**
 * Created by fsuda on 27.11.2017.
 */
public class AgreementErrorEvent extends AgreementEvent {
  public AgreementErrorEvent(Connection con, URI agreementUri) {
    super(con, agreementUri);
  }
}
