package won.node.camel.processor;

import com.hp.hpl.jena.query.DatasetFactory;
import junit.framework.Assert;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import won.node.messaging.processors.CreateNeedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/refactoring/message-processors.xml"})
public class WonMessageSlipComputerTests
{
  @Autowired
  WonMessageSlipComputer wonMessageSlipComputer;

  @Autowired
  CreateNeedMessageProcessor test;

  public void setTest(final CreateNeedMessageProcessor test) {
    this.test = test;
  }

  @Test
  public void testCreateFromOwner() throws Exception {
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CREATE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("createNeedMessageProcessor", exchange.getIn().getHeader("wonSlip"));
  }
  @Test
  public void testActivateNeedMessage() throws Exception {
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_ACTIVATE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WONMSG.TYPE_FROM_OWNER.getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("activateNeedMessageProcessor", exchange.getIn().getHeader
      ("wonSlip"));
  }

  @Test
  public void testCloseMessageFromNode() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CLOSE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("closeMessageFromNodeProcessor,closeFromNodeOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testCloseMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CLOSE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("closeMessageFromOwnerProcessor,closeFromOwnerOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }

  @Test
  public void testConnectMessageFromNode() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CONNECT_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString
      ()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("connectMessageFromNodeProcessor,connectFromNodeOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }

  @Test
  public void testConnectMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CONNECT_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("connectMessageFromOwnerProcessor,connectFromOwnerOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testDeactivateNeedMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_DEACTIVATE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("deactivateNeedMessageProcessor", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testHintMessageProcessor() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_HINT_STRING));
    exchange.getIn().setHeader("direction", URI.create(
      WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("hintMessageProcessor", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testOpenMessageFromNode() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_OPEN_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("openMessageFromNodeProcessor,openFromNodeOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testOpenMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_OPEN_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("openMessageFromOwnerProcessor,openFromOwnerOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testSendMessageFromNode() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CONNECTION_MESSAGE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("sendMessageFromNodeProcessor,sendMessageFromNodeOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testSendMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CONNECTION_MESSAGE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("sendMessageFromOwnerProcessor,sendMessageFromOwnerOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testSendMessageFromNodeGroupFacet() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CONNECTION_MESSAGE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WON.GROUP_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("sendMessageFromNodeProcessor,sendMessageFromNodeGroupFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }
  public void setWonMessageSlipComputer(final WonMessageSlipComputer wonMessageSlipComputer) {
    this.wonMessageSlipComputer = wonMessageSlipComputer;
  }
}
