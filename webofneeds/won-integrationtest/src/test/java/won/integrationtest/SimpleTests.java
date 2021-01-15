package won.integrationtest;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.test.TestFailedEvent;
import won.bot.framework.eventbot.event.impl.test.TestPassedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.Prefixer;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WXBUDDY;
import won.protocol.vocabulary.WXHOLD;
import won.utils.content.model.AtomContent;
import won.utils.content.model.RdfOutput;
import won.utils.content.model.Socket;

import java.lang.invoke.MethodHandles;
import java.net.URI;

public class SimpleTests extends AbstractBotBasedTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void testCreatePersonaWithoutACL() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            WonNodeInformationService wonNodeInformationService = ctx
                            .getWonNodeInformationService();
            final URI atomUri = wonNodeInformationService.generateAtomURI(wonNodeUri);
            String atomUriString = atomUri.toString();
            AtomContent atomContent = new AtomContent(atomUriString);
            atomContent.addTitle("Unit Test Atom ");
            Socket holderSocket = new Socket(atomUriString + "#holderSocket");
            holderSocket.setSocketDefinition(WXHOLD.HolderSocket.asURI());
            Socket buddySocket = new Socket(atomUriString + "#buddySocket");
            buddySocket.setSocketDefinition(WXBUDDY.BuddySocket.asURI());
            atomContent.addSocket(holderSocket);
            atomContent.addSocket(buddySocket);
            atomContent.addType(URI.create(WON.Persona.getURI()));
            atomContent.addType(URI.create(WON.Atom.getURI()));
            Graph contentGraph = RdfOutput.toGraph(atomContent);
            Model contentModel = ModelFactory.createModelForGraph(contentGraph);
            if (logger.isDebugEnabled()) {
                logger.debug("creating atom on won node {} with content: {} ", wonNodeUri,
                                RdfUtils.toString(Prefixer.setPrefixes(contentModel)));
            }
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content().model(contentModel)
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            EventListener successCallback = event -> {
                logger.info(
                                "#####################################################################################");
                logger.info("Persona creation successful, new atom URI is {}", atomUri);
                logger.info(
                                "#####################################################################################");
                bus.publish(new TestPassedEvent(bot));
            };
            EventListener failureCallback = event -> {
                String textMessage = WonRdfUtils.MessageUtils
                                .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                logger.error("Persona creation failed for atom URI {}, original message URI: {}",
                                atomUri, textMessage);
                ctx.getBotContextWrapper().removeAtomUri(atomUri);
                bus.publish(new TestFailedEvent(bot,
                                String.format("Persona creation failed for atom URI %s, original message URI: %s",
                                                atomUri, textMessage)));
            };
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            logger.debug("registered listeners for response to message URI {}",
                            createMessage.getMessageURI());
            ctx.getWonMessageSender().sendMessage(createMessage);
            logger.debug("BotServiceAtom creation message sent with message URI {}",
                            createMessage.getMessageURI());
        });
    }
}
