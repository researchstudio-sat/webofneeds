package won.node.refactoring;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;

/**
 * TODO [msg-refactoring]: not needed. We can implement WonMessageProcessor
 */
public interface FacetCamel
{
  public void process(WonMessage wonMessage)
    throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;
}
