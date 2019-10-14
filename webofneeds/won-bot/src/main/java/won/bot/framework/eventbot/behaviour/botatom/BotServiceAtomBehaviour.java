package won.bot.framework.eventbot.behaviour.botatom;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.bot.context.BotServiceAtomContext;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.lifecycle.InitializeEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WXHOLD;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Behaviour that creates exactly one Atom that represents the Bot itself
 */
public class BotServiceAtomBehaviour extends BotBehaviour {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BotServiceAtomContext serviceAtomContext;
    private final BotServiceAtomContent botServiceAtomContent;

    public BotServiceAtomBehaviour(EventListenerContext context) {
        this(context, context.getBotContextWrapper().getBotName());
    }

    public BotServiceAtomBehaviour(EventListenerContext context, String botServiceName) {
        this(context, new BotServiceAtomContent(botServiceName));
    }

    public BotServiceAtomBehaviour(EventListenerContext context, BotServiceAtomContent botServiceAtomContent) {
        super(context);
        if (!(context.getBotContextWrapper() instanceof BotServiceAtomContext)) {
            throw new IllegalStateException("BotServiceAtomBehaviour does not work without a BotServiceAtomContext");
        }
        this.serviceAtomContext = (BotServiceAtomContext) context.getBotContextWrapper();
        this.botServiceAtomContent = botServiceAtomContent;
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        logger.debug("activating BotServiceAtomBehaviour");
        EventListenerContext ctx = this.context;
        subscribeWithAutoCleanup(InitializeEvent.class,
                        new ActionOnEventListener(context, new AbstractCreateAtomAction(ctx) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                logger.debug("Initializing the BotServiceAtom...");
                                URI serviceAtomUri = serviceAtomContext.getBotServiceAtomUri();
                                if (serviceAtomUri == null) {
                                    logger.debug("BotServiceAtom does not exist, creating a new one...");
                                    final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                                    WonNodeInformationService wonNodeInformationService = ctx
                                                    .getWonNodeInformationService();
                                    final URI atomUri = wonNodeInformationService.generateAtomURI(wonNodeUri);
                                    Dataset botServiceDataset = new BotServiceAtomModelWrapper(atomUri,
                                                    botServiceAtomContent).copyDataset();
                                    logger.debug("creating BotServiceAtom on won node {} with content: {} ", wonNodeUri,
                                                    RdfUtils.toString(botServiceDataset));
                                    WonMessage createAtomMessage = createWonMessage(atomUri, botServiceDataset);
                                    EventBotActionUtils.rememberInList(ctx, atomUri, uriListName);
                                    EventBus bus = ctx.getEventBus();
                                    EventListener successCallback = new EventListener() {
                                        @Override
                                        public void onEvent(Event event) {
                                            logger.debug("#####################################################################################");
                                            logger.debug("BotServiceAtom creation successful, new atom URI is {}",
                                                            atomUri);
                                            logger.debug("#####################################################################################");
                                            serviceAtomContext.setBotServiceAtomUri(atomUri);
                                            bus.publish(new AtomCreatedEvent(atomUri, wonNodeUri, botServiceDataset,
                                                            null));
                                        }
                                    };
                                    EventListener failureCallback = new EventListener() {
                                        @Override
                                        public void onEvent(Event event) {
                                            String textMessage = WonRdfUtils.MessageUtils
                                                            .getTextMessage(((FailureResponseEvent) event)
                                                                            .getFailureMessage());
                                            logger.error("BotServiceAtom creation failed for atom URI {}, original message URI: {}",
                                                            atomUri, textMessage);
                                            EventBotActionUtils.removeFromList(ctx, atomUri, uriListName);
                                        }
                                    };
                                    EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage,
                                                    successCallback, failureCallback, ctx);
                                    logger.debug("registered listeners for response to message URI {}",
                                                    createAtomMessage.getMessageURI());
                                    ctx.getWonMessageSender().sendWonMessage(createAtomMessage);
                                    logger.debug("BotServiceAtom creation message sent with message URI {}",
                                                    createAtomMessage.getMessageURI());
                                } else {
                                    logger.debug("BotServiceAtom exists atomUri: {} checking validity...",
                                                    serviceAtomUri);
                                    Dataset serviceBotDataSet = ctx.getLinkedDataSource()
                                                    .getDataForResource(serviceAtomUri);
                                    if (serviceBotDataSet == null) {
                                        logger.debug("BotServiceAtom can't be retrieved, creating a new one...");
                                        // TODO: CREATE BOT SERVICE ATOM
                                    } else if (false /* TODO CHECK IF SERVICE ATOM IS STILL CORRECT */) {
                                        logger.debug("BotServiceAtom is outdated, modifying the BotServiceAtom");
                                        // TODO: UPDATE BOT SERVICE ATOM
                                    } else {
                                        logger.debug("BotServiceAtom is still up to date.");
                                        // TODO: nothing
                                    }
                                }
                            }
                        }));
        subscribeWithAutoCleanup(AtomCreatedEvent.class,
                        new ActionOnEventListener(context, new BaseEventBotAction(ctx) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                if (event instanceof AtomCreatedEvent) {
                                    AtomCreatedEvent atomCreatedEvent = (AtomCreatedEvent) event;
                                    URI botServiceAtomUri = serviceAtomContext.getBotServiceAtomUri();
                                    URI createdAtomUri = atomCreatedEvent.getAtomURI();
                                    if (!Objects.equals(createdAtomUri, botServiceAtomUri)) {
                                        if (ctx.getBotContext().isAtomKnown(createdAtomUri)) {
                                            logger.debug("Atom ({}) is known, must be one we created..., dataset: {}",
                                                            createdAtomUri,
                                                            RdfUtils.toString(atomCreatedEvent.getAtomDataset()));
                                            DefaultAtomModelWrapper createdAtomModelWrapper = new DefaultAtomModelWrapper(
                                                            atomCreatedEvent.getAtomDataset());
                                            Optional<URI> createdAtomHoldableSocketUri = WonLinkedDataUtils
                                                            .getSocketsOfType(createdAtomUri,
                                                                            URI.create(WXHOLD.HoldableSocketString),
                                                                            ctx.getLinkedDataSource())
                                                            .stream().findFirst();
                                            if (createdAtomHoldableSocketUri.isPresent()) {
                                                logger.debug("Atom ({}) has the holdableSocket, connect botServiceAtom ({}) with this atom",
                                                                createdAtomUri, botServiceAtomUri);
                                                Optional<URI> botServiceAtomHolderSocketUri = WonLinkedDataUtils
                                                                .getSocketsOfType(botServiceAtomUri,
                                                                                URI.create(WXHOLD.HolderSocketString),
                                                                                ctx.getLinkedDataSource())
                                                                .stream().findFirst();
                                                URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(
                                                                ctx.getLinkedDataSource().getDataForResource(
                                                                                createdAtomUri),
                                                                createdAtomUri);
                                                URI remoteWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(
                                                                ctx.getLinkedDataSource().getDataForResource(
                                                                                botServiceAtomUri),
                                                                botServiceAtomUri);
                                                logger.debug("Connecting atom ({}) - botServiceAtom ({})",
                                                                createdAtomUri,
                                                                botServiceAtomUri);
                                                WonMessage connectToServiceAtomMessage = WonMessageBuilder
                                                                .setMessagePropertiesForConnect(
                                                                                ctx.getWonNodeInformationService()
                                                                                                .generateEventURI(
                                                                                                                localWonNode),
                                                                                createdAtomHoldableSocketUri,
                                                                                createdAtomUri, localWonNode,
                                                                                botServiceAtomHolderSocketUri,
                                                                                botServiceAtomUri, remoteWonNode, null)
                                                                .build();
                                                ctx.getWonMessageSender().sendWonMessage(connectToServiceAtomMessage);
                                            } else {
                                                logger.debug("Atom ({}) does not have a holdable Socket, no connect action required",
                                                                createdAtomUri);
                                            }
                                        } else {
                                            logger.debug("Atom ({}) is not known, must be someone elses...",
                                                            createdAtomUri);
                                        }
                                    } else {
                                        logger.debug("BotServiceAtomCreated, no connect action required");
                                    }
                                }
                            }
                        }));
        // TODO: ACCEPT THE HOLDER CONNECTIONS IF REMOTE ATOM IS THE BotServiceAtom
        subscribeWithAutoCleanup(ConnectFromOtherAtomEvent.class,
                        new ActionOnEventListener(context, new BaseEventBotAction(ctx) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                // TODO: ACCEPT CONNECT REQUEST IF HOLDER FACET AND TARGETATOM IS FROM US
                                if (event instanceof ConnectFromOtherAtomEvent) {
                                    ConnectFromOtherAtomEvent connectFromOtherAtomEvent = (ConnectFromOtherAtomEvent) event;
                                    logger.debug("Possibly accept the connection Request with holderFacet<->holdableFacet if atom is BotServiceAtom");
                                    if (ctx.getBotContext().isAtomKnown(connectFromOtherAtomEvent.getAtomURI())
                                                    && ctx.getBotContext().isAtomKnown(
                                                                    connectFromOtherAtomEvent.getTargetAtomURI())) {
                                        logger.debug("Both Atoms belong to you, you might want to accept the connect, socketUri: {}, targetSocketUri: {}",
                                                        connectFromOtherAtomEvent.getCon().getSocketURI(),
                                                        connectFromOtherAtomEvent.getCon().getTargetSocketURI());
                                        logger.debug("TargetAtomUri: {}", connectFromOtherAtomEvent.getTargetAtomURI());
                                        logger.debug("AtomUri: {}", connectFromOtherAtomEvent.getAtomURI());
                                    } else {
                                        logger.debug("At least one of the two Atoms is not known, must be a request from someone else...");
                                    }
                                }
                            }
                        }));
    }
}
