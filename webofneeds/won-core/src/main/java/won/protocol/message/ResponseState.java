package won.protocol.message;

import org.apache.jena.rdf.model.Resource;

import won.protocol.vocabulary.WONMSG;

public enum ResponseState {

  SUCCESS(WONMSG.TYPE_RESPONSE_STATE_SUCCESS), FAILURE(WONMSG.TYPE_RESPONSE_STATE_FAILURE),
  DUPLICATE_NEED_ID(WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_NEED_ID),
  DUPLICATE_CONNECTION_ID(WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_CONNECTION_ID),
  DUPLICATE_MESSAGE_ID(WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_MESSAGE_ID);

  private Resource resource;

  private ResponseState(Resource resource) {
    this.resource = resource;
  }

  public Resource getResource() {
    return resource;
  }

  public static ResponseState getResponseState(Resource resource) {

    if (WONMSG.TYPE_RESPONSE_STATE_SUCCESS.equals(resource))
      return SUCCESS;
    if (WONMSG.TYPE_RESPONSE_STATE_FAILURE.equals(resource))
      return FAILURE;
    if (WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_NEED_ID.equals(resource))
      return DUPLICATE_NEED_ID;
    if (WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_CONNECTION_ID.equals(resource))
      return DUPLICATE_CONNECTION_ID;
    if (WONMSG.TYPE_RESPONSE_STATE_DUPLICATE_MESSAGE_ID.equals(resource))
      return DUPLICATE_MESSAGE_ID;

    return null;
  }

}
