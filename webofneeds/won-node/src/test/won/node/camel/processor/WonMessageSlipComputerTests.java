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
import won.protocol.message.WonEnvelopeType;
import won.protocol.message.WonMessage;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/refactoring/message-processors.xml"})
public class WonMessageSlipComputerTests
{
  @Autowired
  WonMessageSlipComputer wonMessageSlipComputer;

  @Test
  public void testCreateFromOwner() throws Exception {
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CREATE_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonEnvelopeType.FROM_OWNER.getResource().getURI().toString()));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("createNeedMessageProcessor", exchange.getIn().getHeader("wonSlip"));
  }
  @Test
  public void testActivateNeed() throws Exception {
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
  }

  @Test
  public void testCloseMessageFromNode() throws Exception{

  }
  @Test
  public void testCloseMessageFromOwner() throws Exception{

  }

  @Test
  public void testConnectMessageFromNode() throws Exception{

  }

  @Test
  public void testConnectMessageFromOwner() throws Exception{
    Exchange exchange = new DefaultExchange(new DefaultCamelContext());
    exchange.getIn().setHeader("wonMessage", new WonMessage(DatasetFactory.createMem()));
    exchange.getIn().setHeader("messageType", URI.create(WONMSG.TYPE_CONNECT_STRING));
    exchange.getIn().setHeader("direction", URI.create(WonEnvelopeType.FROM_OWNER.getResource().getURI().toString()));
    exchange.getIn().setHeader("facetType",URI.create(WONMSG.OWNER_FACET_STRING));
    wonMessageSlipComputer.evaluate(exchange,String.class);
    Assert.assertEquals("connectMessageFromOwnerProcessor,connectFromOwnerOwnerFacetImpl", exchange.getIn().getHeader
      ("wonSlip"));
  }
  @Test
  public void testDeactivateNeedMessageFromOwner() throws Exception{

  }
  @Test
  public void testHintMessageProcessor() throws Exception{

  }
  @Test
  public void testOpenMessageFromNode() throws Exception{

  }
  @Test
  public void testOpenMessageFromOwner() throws Exception{

  }
  @Test
  public void testSendMessageFromNode() throws Exception{

  }
  @Test
  public void testSendMessageFromOwner() throws Exception{

  }

  public void setWonMessageSlipComputer(final WonMessageSlipComputer wonMessageSlipComputer) {
    this.wonMessageSlipComputer = wonMessageSlipComputer;
  }
}
