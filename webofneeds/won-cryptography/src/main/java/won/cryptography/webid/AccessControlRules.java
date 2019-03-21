package won.cryptography.webid;

import java.util.List;

/**
 * User: ypanchenko
 * Date: 28.07.2015
 */
public interface AccessControlRules
{

  public boolean isAccessPermitted(String resourceURI, List<String> requesterWebIDs);
}
