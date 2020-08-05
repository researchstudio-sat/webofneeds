package won.node.camel.processor;

import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.jena.query.DatasetFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.Assert;
import won.node.camel.processor.fixed.CreateAtomMessageProcessor;
import won.node.camel.processor.general.MessageTypeSlipComputer;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXGROUP;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring/refactoring/message-processors.xml" })
public class WonMessageSlipComputerTests {
    @Autowired
    MessageTypeSlipComputer wonMessageSlipComputer;
    @Autowired
    CreateAtomMessageProcessor test;

    public void setTest(final CreateAtomMessageProcessor test) {
        this.test = test;
    }

    @Test
    public void testCreateFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.CreateMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("createAtomMessageProcessor", slip);
    }

    @Test
    public void testActivateAtomMessage() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ActivateMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WONMSG.FromOwner.getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("activateAtomMessageProcessor", slip);
    }

    @Test
    public void testCloseMessageFromNode() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.CloseMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("closeMessageFromNodeProcessor,closeFromNodeChatSocketImpl", slip);
    }

    @Test
    public void testCloseMessageFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.CloseMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("closeMessageFromOwnerProcessor,closeFromOwnerChatSocketImpl", slip);
    }

    @Test
    public void testConnectMessageFromNode() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("connectMessageFromNodeProcessor,connectFromNodeChatSocketImpl", slip);
    }

    @Test
    public void testConnectMessageFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("connectMessageFromOwnerProcessor,connectFromOwnerChatSocketImpl", slip);
    }

    @Test
    public void testDeactivateAtomMessageFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.DeactivateMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("deactivateAtomMessageProcessor", slip);
    }

    @Test
    public void testHintMessageProcessor() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.SocketHintMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(
                        WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("hintMessageProcessor", slip);
    }

    @Test
    public void testSendMessageFromNode() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectionMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("sendMessageFromNodeProcessor,sendMessageFromNodeChatSocketImpl", slip);
    }

    @Test
    public void testSendMessageFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectionMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("sendMessageFromOwnerProcessor,sendMessageFromOwnerChatSocketImpl", slip);
    }

    @Test
    public void testSendMessageFromNodeGroupSocket() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, WonMessage.of(DatasetFactory.createGeneral()));
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectionMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXGROUP.GroupSocket.asURI());
        String slip = wonMessageSlipComputer.evaluate(exchange, String.class);
        Assert.assertEquals("sendMessageFromNodeProcessor,sendMessageFromNodeGroupSocketImpl", slip);
    }

    public void setWonMessageFixedSlipComputer(final MessageTypeSlipComputer wonMessageFixedSlipComputer) {
        this.wonMessageSlipComputer = wonMessageFixedSlipComputer;
    }
}
