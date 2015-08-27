package hint.actor;

import akka.camel.javaapi.UntypedProducerActor;

/**
 * Created by hfriedrich on 27.08.2015.
 */
public class HintProducerProtocolActor extends UntypedProducerActor
{
  private String endpoint;

  public HintProducerProtocolActor(String endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public String getEndpointUri() {
    return endpoint;
  }


}
