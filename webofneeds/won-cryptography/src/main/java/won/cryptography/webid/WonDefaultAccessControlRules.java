package won.cryptography.webid;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.MessageEventRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
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
    List<String> permittedWebIDs = extractParticipatingParties(resourceURI);
    if (permittedWebIDs == null) {
      // no access control restrictions found, allow access by default (or should be reject by default...?)
      logger.info("found null for permitted WebIDs - allowing access to {}", resourceURI);
      return true;
    }
    Collection<String> requesterPermittedWebIDs = CollectionUtils.intersection(permittedWebIDs, requesterWebIDs);
    if (logger.isDebugEnabled()) {
      logger.debug("found requester WebIDs permitted to access {}: {}", resourceURI, requesterPermittedWebIDs.toString
        ());
    }
    return requesterPermittedWebIDs.size() > 0;
  }

  private List<String> extractParticipatingParties(String resourceUri) {
    MessageEventPlaceholder event = messageEventRepository.findOneByMessageURI(URI.create(resourceUri));
    if (event == null) {
      return null;
    }
    List<String> sendersAndReceivers = new ArrayList<>(4);
    if (event.getSenderNeedURI() != null) {
      sendersAndReceivers.add(event.getSenderNeedURI().toString());
    }
    if (event.getReceiverNeedURI() != null) {
      sendersAndReceivers.add(event.getReceiverNeedURI().toString());
    }
    if (event.getSenderNodeURI() != null) {
      sendersAndReceivers.add(event.getSenderNodeURI().toString());
    }
    if (event.getReceiverNodeURI() != null) {
      sendersAndReceivers.add(event.getReceiverNodeURI().toString());
    }
    return sendersAndReceivers;
  }
}
