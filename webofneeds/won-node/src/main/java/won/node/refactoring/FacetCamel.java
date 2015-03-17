package won.node.refactoring;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;

/**
 * User: syim
 * Date: 05.03.2015
 */
public interface FacetCamel
{
  public void process(WonMessage wonMessage)
    throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;
}
