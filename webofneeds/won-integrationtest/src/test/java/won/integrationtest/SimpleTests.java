package won.integrationtest;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.test.TestPassedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WXBUDDY;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXHOLD;
import won.utils.content.model.AtomContent;
import won.utils.content.model.RdfOutput;
import won.utils.content.model.Socket;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static won.bot.framework.eventbot.action.EventBotActionUtils.makeAction;

public class SimpleTests extends AbstractBotBasedTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test(timeout = 60 * 1000)
    public void testCreatePersonaWithoutACL() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = new AtomContent(atomUriString);
            atomContent.addTitle("Unit Test Atom ");
            final Socket holderSocket = new Socket(atomUriString + "#holderSocket");
            holderSocket.setSocketDefinition(WXHOLD.HolderSocket.asURI());
            final Socket buddySocket = new Socket(atomUriString + "#buddySocket");
            buddySocket.setSocketDefinition(WXBUDDY.BuddySocket.asURI());
            atomContent.addSocket(holderSocket);
            atomContent.addSocket(buddySocket);
            atomContent.addType(URI.create(WON.Persona.getURI()));
            atomContent.addType(URI.create(WON.Atom.getURI()));
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content().graph(RdfOutput.toGraph(atomContent))
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = makeSuccessCallbackToPassTest(bot, bus, action);
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testQueryBasedMatch() throws Exception {
        final AtomicBoolean atom1Created = new AtomicBoolean(false);
        final AtomicBoolean atom2Created = new AtomicBoolean(false);
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            {
                final String atomUriString = atomUri1.toString();
                final AtomContent atomContent = new AtomContent(atomUriString);
                atomContent.addTitle("Match target atom");
                atomContent.addTag("tag-to-match");
                final Socket chatSocket = new Socket(atomUriString + "#chatSocket");
                chatSocket.setSocketDefinition(WXCHAT.ChatSocket.asURI());
                atomContent.addSocket(chatSocket);
                atomContent.addType(URI.create(WON.Atom.getURI()));
                WonMessage createMessage = WonMessageBuilder.createAtom()
                                .atom(atomUri1)
                                .content().graph(RdfOutput.toGraph(atomContent))
                                .build();
                createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                final String action = "Create match target atom";
                EventListener successCallback = event -> {
                    logger.debug("Match target atom created");
                    atom1Created.set(true);
                };
                EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                action);
                EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                failureCallback, ctx);
                ctx.getWonMessageSender().sendMessage(createMessage);
            }
            // create match source
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            {
                final String atomUriString = atomUri2.toString();
                final AtomContent atomContent = new AtomContent(atomUriString);
                atomContent.addTitle("Match source atom");
                atomContent.addSparqlQuery(
                                "PREFIX won:<https://w3id.org/won/core#>\n"
                                                + "PREFIX con:<https://w3id.org/won/content#>\n"
                                                + "SELECT ?result (1.0 AS ?score) WHERE {"
                                                + "?result a won:Atom ;"
                                                + "    con:tag \"tag-to-match\"."
                                                + "}");
                final Socket chatSocket = new Socket(atomUriString + "#chatSocket");
                chatSocket.setSocketDefinition(WXCHAT.ChatSocket.asURI());
                atomContent.addSocket(chatSocket);
                atomContent.addType(URI.create(WON.Atom.getURI()));
                WonMessage createMessage = WonMessageBuilder.createAtom()
                                .atom(atomUri2)
                                .content().graph(RdfOutput.toGraph(atomContent))
                                .build();
                createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                final String action = "Create match source atom";
                EventListener successCallback = event -> {
                    logger.debug("Match source atom created");
                    atom2Created.set(true);
                };
                EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                action);
                EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                failureCallback, ctx);
                ctx.getWonMessageSender().sendMessage(createMessage);
            }
            // create listener waiting for the hint
            bus.subscribe(HintFromMatcherEvent.class,
                            (Event event) -> ((HintFromMatcherEvent) event).getRecipientAtom().equals(atomUri2)
                                            && ((HintFromMatcherEvent) event).getHintTargetAtom().equals(atomUri1),
                            makeAction(ctx, event -> bus.publish(new TestPassedEvent(bot))));
            logger.debug("Finished initializing test 'testQueryBasedMatch()'");
        });
    }
}
