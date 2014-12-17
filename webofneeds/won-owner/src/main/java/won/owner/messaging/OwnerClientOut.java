package won.owner.messaging;

import won.protocol.message.WonMessage;

/**
 * User: fsalcher
 * Date: 21.08.2014
 */
public interface OwnerClientOut
{
  public void sendMessage(WonMessage wonMessage);
}
