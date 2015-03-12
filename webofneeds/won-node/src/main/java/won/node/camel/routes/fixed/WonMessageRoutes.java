package won.node.camel.routes.fixed;

import org.apache.camel.builder.RouteBuilder;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class WonMessageRoutes  extends RouteBuilder
{

  @Override
  public void configure() throws Exception {
    from("activemq:queue:WonMessageOwnerProtocol.in?concurrentConsumers=5")
      .routeId("WonMessageOwnerRoute")
      .to("bean:ownerProtocolIncomingMessageCamelProcessor")
      .to("bean:wonMessageCamelProcessor")
      .routingSlip(method("wonMessageSlipComputer"))
      .choice()
      .when(header("methodName").isEqualTo("register"))
      .to("bean:ownerManagementService?method=registerOwnerApplication")
      .when(header("methodName").isEqualTo("getEndpoints"))
      .to("bean:queueManagementService?method=getEndpointsForOwnerApplication")
      .wireTap("bean:messagingService?method=inspectMessage");
    from("activemq:queue:WonMessageOwnerProtoco11l.in?concurrentConsumers=5")
      .routeId("WonMessageOwnerRoute2")
      .to("bean:ownerProtocolIncomingMessageCamelProcessor")
      .to("bean:wonMessageCamelProcessor")
      .wireTap("bean:messagingService?method=inspectMessage")
      .choice()
      .when(header("methodName").isEqualTo("register"))
      .to("bean:ownerManagementService?method=registerOwnerApplication")
      .when(header("methodName").isEqualTo("getEndpoints"))
      .to("bean:queueManagementService?method=getEndpointsForOwnerApplication")
      .when(header("messageType").isEqualTo(URI.create(WONMSG.TYPE_CREATE.getURI())))
      .to("bean:createNeedMessageProcessor?method=process")
      .when(header("messageType").isEqualTo(URI.create(WONMSG.TYPE_ACTIVATE.getURI())))
      .to("bean:activateNeedMessageProcessor?method=process")
      .when(header("messageType").isEqualTo(URI.create(WONMSG.TYPE_DEACTIVATE.getURI())))
      .to("bean:deactivateNeedMessageProcessor?method=process")
      .when(header("messageType").isEqualTo(URI.create(WONMSG.TYPE_CLOSE.getURI())))
      .to("bean:closeMessageFromOwnerMessageProcessor?method=process")
      .to("bean:messageDynamicRouter?method=route")
      .when(header("messageType").isEqualTo(URI.create(WONMSG.TYPE_CONNECT.getURI())))
      .to("bean:connectMessageFromOwnerMessageProcessor?method=process")
      .to("bean:messageDynamicRouter?method=route")
      .when(header("messageType").isEqualTo(URI.create(WONMSG.TYPE_OPEN.getURI())))
      .to("bean:openMessageFromOwnerMessageProcessor?method=process")
      .to("bean:messageDynamicRouter?method=route")
      .when(header("messageType").isEqualTo(URI.create(WONMSG.TYPE_CONNECTION_MESSAGE.getURI())))
      .to("bean:sendMessageFromOwnerMessageProcessor?method=process")
      .to("bean:messageDynamicRouter?method=route");
      /*.otherwise()
      .to("bean:closeMessageFromOwnerMessageProcessor?method=process")
      .to("bean:messageDynamicRouter?method=route");*/
    from("activemq:queue:WonMessageNeedProtocol.in?concurrentConsumers=5")
       .routeId("WonMessageNodeRoute")
       .to("bean:wonMessageCamelProcessor")
       .wireTap("bean:messagingService?method=inspectMessage")
       .choice()
       .when(header("messageType").isEqualTo(WONMSG.TYPE_CLOSE))
       .to("bean:closeMessageFromNodeMessageProcessor?method=process")
       .when(header("messageType").isEqualTo(WONMSG.TYPE_CONNECT))
       .to("bean:connectMessageFromNodeMessageProcessor?method=process")
       .when(header("messageType").isEqualTo(WONMSG.TYPE_OPEN))
       .to("bean:openMessageFromNodeMessageProcessor?method=process")
       .when(header("messageType").isEqualTo(WONMSG.TYPE_CONNECTION_MESSAGE))
       .to("bean:sendMessageFromNodeMessageProcessor?method=process");
    from("activemq:queue:WonMessageMatcherProtocol.in?concurrentConsumers=5")
      .routeId("WonMessageMatcherRoute")
      .to("bean:wonMessageCamelProcessor")
      .wireTap("bean:messagingService?method=inspectMessage")
      .choice()
      .when(header("messageType").isEqualTo(WONMSG.TYPE_HINT))
      .to("bean:hintMessageProcessor?method=process");
  }
}
