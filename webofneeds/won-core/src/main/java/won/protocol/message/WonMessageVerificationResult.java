package won.protocol.message;

/**
 * User: fsalcher
 * Date: 02.09.2014
 */
public class WonMessageVerificationResult
{

  public boolean couldNotReadSerializedRDF = false;
  public boolean noEnvelopGraphFound = false;
  public boolean multipleEnvelopsFound = false;
  public boolean noMessageEventTypeFound = false;


}
