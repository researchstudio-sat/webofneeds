package won.protocol.agreement.effect;

import java.net.URI;

public class Rejects extends MessageEffect {

  private URI rejectedMessageUri;

  public Rejects(URI messageUri, URI rejectedMessageUri) {
    super(messageUri, MessageEffectType.REJECTS);
    this.rejectedMessageUri = rejectedMessageUri;
  }

  public URI getRejectedMessageUri() {
    return rejectedMessageUri;
  }

  @Override
  public String toString() {
    return "Rejects [rejectedMessageUri=" + rejectedMessageUri + "]";
  }

}
