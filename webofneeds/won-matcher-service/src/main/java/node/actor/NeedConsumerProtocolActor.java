package node.actor;

import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedConsumerActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import common.config.CommonSettings;
import common.config.CommonSettingsImpl;
import common.service.SparqlService;
import common.event.NeedEvent;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import won.protocol.util.RdfUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Camel actor represents the need consumer protocol to a won node.
 * It is used to listen at the WoN node for events about new created needs,
 * activated needs and deactivated needs. Depending on the endpoint it
 * might be used with different protocols (e.g. JMS over activemq).
 *
 * User: hfriedrich
 * Date: 28.04.2015
 */
public class NeedConsumerProtocolActor extends UntypedConsumerActor
{
  private static final String MSG_HEADER_METHODNAME = "methodName";
  private static final String MSG_HEADER_METHODNAME_NEEDCREATED = "needCreated";
  private static final String MSG_HEADER_METHODNAME_NEEDACTIVATED = "needActivated";
  private static final String MSG_HEADER_METHODNAME_NEEDDEACTIVATED = "needDeactivated";
  private static final String MSG_HEADER_WON_NODE_URI = "wonNodeURI";
  private static final String MSG_HEADER_NEED_URI = "needUri";
  private final CommonSettingsImpl settings = CommonSettings.SettingsProvider.get(getContext().system());
  private SparqlService sparqlService;
  private final String endpoint;
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  public NeedConsumerProtocolActor(String endpoint) {
    this.endpoint = endpoint;
    this.sparqlService = new SparqlService(settings.SPARQL_ENDPOINT);
  }

  @Override
  public String getEndpointUri() {
    return endpoint;
  }

  @Override
  public void onReceive(final Object message) throws Exception {

    if (message instanceof CamelMessage) {
      CamelMessage camelMsg = (CamelMessage) message;
      String needUri = (String) camelMsg.getHeaders().get(MSG_HEADER_NEED_URI);
      String wonNodeUri = (String) camelMsg.getHeaders().get(MSG_HEADER_WON_NODE_URI);
      if (needUri != null && wonNodeUri != null) {
        Object methodName = camelMsg.getHeaders().get(MSG_HEADER_METHODNAME);
        if (methodName != null) {
          log.debug("Received event '{}' for needUri '{}' and wonNeedUri '{}'", methodName, needUri, wonNodeUri);

          // save the need
          Dataset ds = convertBodyToDataset(camelMsg.body(), Lang.TRIG);
          sparqlService.updateDataset(ds);

          // publish an internal need event
          NeedEvent event = null;
          if (methodName.equals(MSG_HEADER_METHODNAME_NEEDCREATED)) {
            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.CREATED, camelMsg.body().toString(), Lang.TRIG);
            getContext().system().eventStream().publish(event);
            return;
          } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDACTIVATED)) {
            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.ACTIVATED, camelMsg.body().toString(), Lang.TRIG);
            getContext().system().eventStream().publish(event);
            return;
          } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDDEACTIVATED)) {
            event = new NeedEvent(needUri, wonNodeUri, NeedEvent.TYPE.DEACTIVATED, camelMsg.body().toString(), Lang.TRIG);
            getContext().system().eventStream().publish(event);
            return;
          }
        }
      }
    } else {
      System.out.print("some other message");
    }
    unhandled(message);
  }

  private Dataset convertBodyToDataset(Object body, Lang lang) {
    InputStream is = new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.UTF_8));
    return RdfUtils.toDataset(is, new RDFFormat(lang));
  }

}
