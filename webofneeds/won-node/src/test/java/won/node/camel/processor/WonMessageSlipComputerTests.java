package won.node.camel.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.Assert;
import org.springframework.transaction.PlatformTransactionManager;
import won.cryptography.rdfsign.WebIdKeyLoader;
import won.cryptography.service.CryptographyService;
import won.cryptography.service.RandomNumberService;
import won.cryptography.service.RegistrationClient;
import won.node.camel.AtomProtocolCommunicationServiceImpl;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.node.camel.processor.general.MessageTypeSlipComputer;
import won.node.camel.service.CamelWonMessageService;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.node.service.persistence.AtomService;
import won.node.service.persistence.ConnectionService;
import won.node.service.persistence.MessageService;
import won.node.service.persistence.OwnerManagementService;
import won.node.service.persistence.SocketService;
import won.protocol.jms.ActiveMQService;
import won.protocol.jms.AtomProtocolCamelConfigurator;
import won.protocol.jms.AtomProtocolCommunicationService;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageContainerRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.service.MessageRoutingInfoService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXGROUP;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(name = "parent", locations = {
                "classpath:/spring/component/camel/message-processors.xml",
                "classpath:/spring/component/camel/node-camel.xml"
})
@MockBean(CamelContext.class) // replace the camelcontext from node-camel.xml
@MockBean(name = "transactionManager", classes = { PlatformTransactionManager.class })
@MockBean(name = "registrationClient", classes = { RegistrationClient.class })
@MockBean(name = "atomProtocolCommunicationService", classes = { AtomProtocolCommunicationServiceImpl.class })
@MockBean(name = "messageContainerRepository", classes = { MessageContainerRepository.class })
@MockBean(name = "messageRoutingInfoService", classes = { MessageRoutingInfoService.class })
@MockBean(name = "ownerManagementService", classes = { OwnerManagementService.class })
@MockBean(name = "webIdKeyLoader", classes = { WebIdKeyLoader.class })
@MockBean(name = "activeMQService", classes = { ActiveMQService.class })
@MockBean(name = "entityManager", classes = { EntityManager.class })
@MockBean(name = "messagingService", classes = { MessagingService.class })
@MockBean(name = "atomProtocolCamelConfigurator", classes = { AtomProtocolCamelConfigurator.class })
@MockBean(name = "datasetHolderRepository", classes = { DatasetHolderRepository.class })
@MockBean(name = "atomRepository", classes = { AtomRepository.class })
@MockBean(name = "connectionContainerRepository", classes = { ConnectionContainerRepository.class })
@MockBean(name = "atomMessageContainerRepository", classes = { AtomMessageContainerRepository.class })
@MockBean(name = "connectionRepository", classes = { ConnectionRepository.class })
@MockBean(name = "connectionMessageContainerRepository", classes = { ConnectionMessageContainerRepository.class })
@MockBean(name = "socketRepository", classes = { SocketRepository.class })
@MockBean(name = "ownerApplicationRepository", classes = { OwnerApplicationRepository.class })
@MockBean(name = "messageEventRepository", classes = { MessageEventRepository.class })
@MockBean(name = "wonNodeInformationService", classes = { WonNodeInformationService.class })
@MockBean(name = "linkedDataSource", classes = { LinkedDataSource.class })
@MockBean(name = "matcherProtocolMatcherClient", classes = { MatcherProtocolMatcherServiceClientSide.class })
@MockBean(name = "randomNumberService", classes = { RandomNumberService.class })
@MockBean(name = "executorService", classes = { ExecutorService.class })
@MockBean(name = "socketService", classes = { SocketService.class })
@MockBean(name = "atomService", classes = { AtomService.class })
@MockBean(name = "connectionService", classes = { ConnectionService.class })
@MockBean(name = "messageService", classes = { MessageService.class })
@MockBean(name = "camelWonMessageService", classes = { CamelWonMessageService.class })
@MockBean(name = "cryptographyService", classes = { CryptographyService.class })
@TestPropertySource(locations = { "classpath:/won/node/PersistenceTest.properties" })
public class WonMessageSlipComputerTests {
    @Autowired
    MessageTypeSlipComputer fixedMessageProcessorSlip;
    WonMessage dummyMessage;

    @Before
    public void setUp() throws IOException {
        // the message content does not matter, but we need one
        dummyMessage = makeCreateAtomMessage(URI.create("https://example.com/won/resource/atom/atom1"),
                        "/won/node/WonMessageRoutesTest/data/test-atom1.ttl");
    }

    @Test
    public void testCreateFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.CreateMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:createAtomMessageProcessor?method=process", slip);
    }

    @Test
    public void testActivateAtomMessage() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ActivateMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WONMSG.FromOwner.getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:activateAtomMessageProcessor?method=process", slip);
    }

    @Test
    public void testCloseMessageFromNode() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.CloseMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:closeMessageFromNodeProcessor?method=process", slip);
    }

    @Test
    public void testCloseMessageFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.CloseMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:closeMessageFromOwnerProcessor?method=process", slip);
    }

    @Test
    public void testConnectMessageFromNode() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:connectMessageFromNodeProcessor?method=process", slip);
    }

    @Test
    public void testConnectMessageFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:connectMessageFromOwnerProcessor?method=process", slip);
    }

    @Test
    public void testDeactivateAtomMessageFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.DeactivateMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:deactivateAtomMessageProcessor?method=process", slip);
    }

    @Test
    public void testSendMessageFromNode() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectionMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:sendMessageFromNodeProcessor?method=process", slip);
    }

    @Test
    public void testSendMessageFromOwner() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectionMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_OWNER.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXCHAT.ChatSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:sendMessageFromOwnerProcessor?method=process", slip);
    }

    @Test
    public void testSendMessageFromNodeGroupSocket() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, dummyMessage);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(WONMSG.ConnectionMessageString));
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER,
                        URI.create(WonMessageDirection.FROM_EXTERNAL.getResource().getURI().toString()));
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_URI_HEADER, WXGROUP.GroupSocket.asURI());
        String slip = fixedMessageProcessorSlip.evaluate(exchange, String.class);
        Assert.assertEquals("bean:sendMessageFromNodeProcessor?method=process", slip);
    }

    public void setWonMessageFixedSlipComputer(final MessageTypeSlipComputer wonMessageFixedSlipComputer) {
        this.fixedMessageProcessorSlip = wonMessageFixedSlipComputer;
    }

    private WonMessage makeCreateAtomMessage(URI atomURI, String filename) throws IOException {
        Dataset atom1Content = loadDatasetAndReplaceAtomURI(filename, atomURI);
        WonMessage createAtom1Msg = WonMessageBuilder
                        .createAtom()
                        .atom(atomURI)
                        .content()
                        /**/.dataset(atom1Content)
                        .build();
        return createAtom1Msg;
    }

    private Dataset loadDatasetAndReplaceAtomURI(String file, URI atomURI) throws IOException {
        Dataset atomDataset = loadDataset(file);
        AtomModelWrapper amw = new AtomModelWrapper(atomDataset);
        amw.setAtomURI(atomURI);
        return atomDataset;
    }

    /**
     * Loads the Dataset.
     *
     * @param resourceName
     * @return
     * @throws IOException
     */
    private Dataset loadDataset(String resourceName) throws IOException {
        return loadDataset(resourceName, null, null);
    }

    /**
     * Loads the Dataset, replacing all occurrences of <code>search</code> by
     * <code>replacement</code>.
     *
     * @param resourceName
     * @param search
     * @param replacement
     * @return
     * @throws IOException
     */
    private Dataset loadDataset(String resourceName, String search, String replacement) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw, StandardCharsets.UTF_8.name());
        String content = null;
        if (search != null && replacement != null) {
            content = sw.toString().replaceAll(Pattern.quote(search), replacement);
        } else {
            content = sw.toString();
        }
        Dataset dataset = DatasetFactory.createGeneral();
        dataset.begin(ReadWrite.WRITE);
        RDFDataMgr.read(dataset, new ByteArrayInputStream(content.getBytes()), RDFFormat.TRIG.getLang());
        is.close();
        dataset.commit();
        return dataset;
    }
}
