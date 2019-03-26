package won.protocol.agreement;

import java.net.URI;

/**
 * Indicates that the conversation data analyzed is incomplete. This is
 * discovered if a message refers to another one, but that message is not
 * contained in the dataset. The URIs of both messages are set in the exception.
 * 
 * 
 * @author fkleedorfer
 *
 */
public class IncompleteConversationDataException extends RuntimeException {

  URI referringMessageUri;
  URI missingMessageUri;

  public IncompleteConversationDataException(URI referringMessageUri, URI missingMessageUri, String predicate) {
    super("message " + referringMessageUri + " refers to other " + missingMessageUri + " via " + predicate
        + ", but that other message is not present in the conversation");
    this.referringMessageUri = referringMessageUri;
    this.missingMessageUri = missingMessageUri;
  }

  public URI getReferringMessageUri() {
    return referringMessageUri;
  }

  public URI getMissingMessageUri() {
    return missingMessageUri;
  }

}
