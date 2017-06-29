package won.cryptography.webid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.repository.MessageEventRepository;

import java.net.URI;
import java.util.List;

/**
 * User: ypanchenko
 * Date: 28.07.2015
 */
public class WonDefaultAccessControlRules implements AccessControlRules
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  //TODO this is tepmorary, untill the acl source is defined
  @Autowired
  protected MessageEventRepository messageEventRepository;

  public WonDefaultAccessControlRules() {
  }

  public boolean isAccessPermitted(String resourceURI, List<String> requesterWebIDs) {
    //TODO retrieve from an acl source for a resource instead of this temporary approach
    //specific for the message event resources
    resourceURI = resourceURI.replace("/data/event/", "/resource/event/");
    resourceURI = resourceURI.replace("/page/event/", "/resource/event/");
    String firstWebId = requesterWebIDs.get(0);
    if (requesterWebIDs.size() > 1){
      logger.warn("received more than 1 requester webids, only using first one: ", firstWebId);
    }
    return messageEventRepository.isReadPermittedForWebID(URI.create(resourceURI), URI.create(firstWebId));
  }

}
