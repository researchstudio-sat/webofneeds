package won.node.camel.processor;

import org.apache.jena.query.DatasetFactory;
import junit.framework.Assert;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import won.node.camel.processor.fixed.CreateNeedMessageProcessor;
import won.node.camel.processor.general.MessageTypeSlipComputer;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/refactoring/message-processors.xml"})
public class WonMessageSlipComputerTests
{
  @Autowired
  MessageTypeSlipComputer wonMessageSlipComputer;

  @Autowired
  CreateNeedMessageProcessor test;

  public void setTest(final CreateNeedMessageProcessor test) {
    this.test = test;
  }

  @Test
  public void testCreateFromOwner() throws Exception {
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_CREATE_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("createNeedMessageProcessor", slip);
  }
  @Test
  public void testActivateNeedMessage() throws Exception {
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_ACTIVATE_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WONMSG.TYPE_FROM_OWNER.getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("activateNeedMessageProcessor", slip);
  }

  @Test
  public void testCloseMessageFromNode() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_CLOSE_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("closeMessageFromNodeProcessor,closeFromNodeChatFacetImpl",slip);
  }
  @Test
  public void testCloseMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_CLOSE_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("closeMessageFromOwnerProcessor,closeFromOwnerChatFacetImpl", slip);
  }

  @Test
  public void testConnectMessageFromNode() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_CONNECT_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString
      ()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("connectMessageFromNodeProcessor,connectFromNodeChatFacetImpl", slip);
  }

  @Test
  public void testConnectMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_CONNECT_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("connectMessageFromOwnerProcessor,connectFromOwnerChatFacetImpl", slip);
  }
  @Test
  public void testDeactivateNeedMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_DEACTIVATE_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("deactivateNeedMessageProcessor", slip);
  }
  @Test
  public void testHintMessageProcessor() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_HINT_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(
      WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("hintMessageProcessor", slip);
  }
  @Test
  public void testOpenMessageFromNode() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_OPEN_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("openMessageFromNodeProcessor,openFromNodeChatFacetImpl", slip);
  }
  @Test
  public void testOpenMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_OPEN_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("openMessageFromOwnerProcessor,openFromOwnerChatFacetImpl", slip);
  }
  @Test
  public void testSendMessageFromNode() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_CONNECTION_MESSAGE_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("sendMessageFromNodeProcessor,sendMessageFromNodeChatFacetImpl", slip);
  }
  @Test
  public void testSendMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_CONNECTION_MESSAGE_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.CHAT_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("sendMessageFromOwnerProcessor,sendMessageFromOwnerChatFacetImpl",slip);
  }
  @Test
  public void testSendMessageFromNodeGroupFacet() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, new WonMessage(DatasetFactory.createGeneral()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.TYPE_CONNECTION_MESSAGE_STRING));
    exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader(WonCamelConstants.FACET_TYPE_HEADER,URI.create(WON.GROUP_FACET_STRING));
    String slip = wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("sendMessageFromNodeProcessor,sendMessageFromNodeGroupFacetImpl", slip);
  }
  public void setWonMessageFixedSlipComputer(final MessageTypeSlipComputer wonMessageFixedSlipComputer) {
    this.wonMessageSlipComputer = wonMessageFixedSlipComputer;
  }
}
