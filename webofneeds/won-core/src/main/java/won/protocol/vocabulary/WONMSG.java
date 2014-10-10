package won.protocol.vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import java.net.URI;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WONMSG
{

  // TODO check with existing code how they do it, do they have ontology objects and
  // access the vocabulary from there? If yes, change to that all the enum classes

  public static final String BASE_URI = "http://purl.org/webofneeds/message#";
  public static final String DEFAULT_PREFIX = "msg";

  private static Model m = ModelFactory.createDefaultModel();


  // main types
  public static final Resource TYPE_CREATE = m.createResource(BASE_URI + "CreateMessage");
  public static final Resource TYPE_CONNECT = m.createResource(BASE_URI + "ConnectMessage");
  public static final Resource TYPE_DEACTIVATE = m.createResource(BASE_URI + "DeactivateMessage");
  public static final Resource TYPE_ACTIVATE = m.createResource(BASE_URI + "ActivateMessage");
  public static final Resource TYPE_OPEN = m.createResource(BASE_URI + "OpenMessage");
  public static final Resource TYPE_CLOSE = m.createResource(BASE_URI + "CloseMessage");
  public static final Resource TYPE_CONNECTION_MESSAGE = m.createResource(BASE_URI + "ConnectionMessage");
  public static final Resource TYPE_HINT = m.createResource(BASE_URI + "HintMessage");

  // notification types
  public static final Resource TYPE_HINT_NOTIFICATION = m.createResource(BASE_URI + "HintNotificationMessage");
  public static final Resource TYPE_NEED_CREATED_NOTIFICATION = m.createResource(BASE_URI +
    "NeedCreatedNotificationMessage");

  // response types
  public static final Resource TYPE_CREATE_RESPONSE = m.createResource(BASE_URI + "CreateResponseMessage");
  public static final Resource TYPE_CONNECT_RESPONSE = m.createResource(BASE_URI + "ConnectResponseMessage");
  public static final Resource TYPE_NEED_STATE_RESPONSE = m.createResource(BASE_URI + "NeedStateResponseMessage");
  public static final Resource TYPE_CLOSE_RESPONSE = m.createResource(BASE_URI + "CloseResponseMessage");
  public static final Resource TYPE_OPEN_RESPONSE = m.createResource(BASE_URI + "OpenResponseMessage");
  public static final Resource TYPE_CONNECTION_MESSAGE_RESPONSE =
      m.createResource(BASE_URI + "ConnectionMessageResponseMessage");

  public static boolean isResponseMessageType(Resource resource) {
    if (resource.equals(TYPE_CREATE_RESPONSE) ||
      resource.equals(TYPE_CONNECT_RESPONSE) ||
      resource.equals(TYPE_NEED_STATE_RESPONSE) ||
      resource.equals(TYPE_CLOSE_RESPONSE) ||
      resource.equals(TYPE_OPEN_RESPONSE) ||
      resource.equals(TYPE_CONNECTION_MESSAGE_RESPONSE))
      return true;
    else
      return false;
  }

  // response states
  public static final Resource TYPE_RESPONSE_STATE_SUCCESS = m.createResource(BASE_URI + "SuccessResponse");
  public static final Resource TYPE_RESPONSE_STATE_FAILURE = m.createResource(BASE_URI + "FailureResponse");
  public static final Resource TYPE_RESPONSE_STATE_DUPLICATE_NEED_ID =
    m.createResource(BASE_URI + "DuplicateNeedIdResponse");
  public static final Resource TYPE_RESPONSE_STATE_DUPLICATE_CONNECTION_ID =
    m.createResource(BASE_URI + "DuplicateConnectionIdResponse");
  public static final Resource TYPE_RESPONSE_STATE_DUPLICATE_MESSAGE_ID =
    m.createResource(BASE_URI + "DuplicateMessageIdResponse");

  public static final Property HAS_RESPONSE_STATE_PROPERTY =
    m.createProperty(BASE_URI + "hasResponseStateProperty"); //TODO rename!


  //public static final String MESSAGE_TYPE_CREATE_RESOURCE = BASE_URI + "CreateMessage";
  //public static final String MESSAGE_TYPE_CONNECT_RESOURCE = BASE_URI + "ConnectMessage";
  //public static final String MESSAGE_TYPE_NEED_STATE_RESOURCE = BASE_URI + "NeedStateMessage";

  public static final Resource ENVELOPE_GRAPH = m.createResource(BASE_URI+ "EnvelopeGraph");

  public static final Property RECEIVER_PROPERTY = m.createProperty(BASE_URI, "hasReceiver");
  public static final Property RECEIVER_NEED_PROPERTY = m.createProperty(BASE_URI, "hasReceiverNeed");
  public static final Property RECEIVER_NODE_PROPERTY = m.createProperty(BASE_URI, "hasReceiverNode");
  public static final Property SENDER_PROPERTY = m.createProperty(BASE_URI, "hasSender");
  public static final Property SENDER_NEED_PROPERTY = m.createProperty(BASE_URI, "hasSenderNeed");
  public static final Property SENDER_NODE_PROPERTY = m.createProperty(BASE_URI, "hasSenderNode");

  public static final Property HAS_MESSAGE_TYPE_PROPERTY = m.createProperty(BASE_URI, "hasMessageType");
  public static final Property HAS_CONTENT_PROPERTY = m.createProperty(BASE_URI, "hasContent");
  public static final Property REFERS_TO_PROPERTY = m.createProperty(BASE_URI, "refersTo");
  public static final Property NEW_NEED_STATE_PROPERTY = m.createProperty(BASE_URI, "newNeedState");
  public static final Property HAS_TIMESTAMP = m.createProperty(BASE_URI, "hasTimestamp");
  //public static final String MESSAGE_HAS_CONTENT_PROPERTY = "hasContent";
  //public static final String MESSAGE_REFERS_TO_PROPERTY = "refersTo";
  //public static final String MESSAGE_NEW_NEED_STATE_PROPERTY = "newNeedState";

  public static final String GRAPH_URI_FRAGMENT = "data";

  /**
   * Returns the base URI for this schema.
   *
   * @return the URI for this schema
   */
  public static String getURI()
  {
    return BASE_URI;
  }

  public static Resource toResource(URI uri){
    return m.getResource(uri.toString());
  }

}
