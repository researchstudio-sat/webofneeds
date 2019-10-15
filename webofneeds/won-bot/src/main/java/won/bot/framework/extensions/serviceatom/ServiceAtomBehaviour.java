package won.bot.framework.extensions.serviceatom;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ServiceAtomBehaviour extends BotBehaviour {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ServiceAtomContext serviceAtomContext;
    private final ServiceAtomContent serviceAtomContent;

    public ServiceAtomBehaviour(EventListenerContext context) {
        this(context, context.getBotContextWrapper().getBotName());
    }

    public ServiceAtomBehaviour(EventListenerContext context, String botServiceName) {
        this(context, new ServiceAtomContent(botServiceName));
    }

    public ServiceAtomBehaviour(EventListenerContext context, ServiceAtomContent serviceAtomContent) {
        super(context);
        if (!(context.getBotContextWrapper() instanceof ServiceAtomContext)) {
            throw new IllegalStateException("ServiceAtomBehaviour does not work without a ServiceAtomContext");
        }
        this.serviceAtomContext = (ServiceAtomContext) context.getBotContextWrapper();
        this.serviceAtomContent = serviceAtomContent;
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        logger.debug("activating ServiceAtomBehaviour");
        final EventListenerContext ctx = this.context;
        subscribeWithAutoCleanup(InitializeEvent.class,
                        new ActionOnEventListener(ctx, new AbstractCreateAtomAction(ctx) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                logger.debug("Initializing the BotServiceAtom...");
                                URI serviceAtomUri = serviceAtomContext.getServiceAtomUri();
                                if (serviceAtomUri == null) {
                                    logger.debug("BotServiceAtom does not exist, creating a new one...");
                                    createServiceAtom();
                                } else {
                                    logger.debug("BotServiceAtom exists atomUri: {} checking validity...",
                                                    serviceAtomUri);
                                    Dataset serviceBotDataSet = ctx.getLinkedDataSource()
                                                    .getDataForResource(serviceAtomUri);
                                    if (serviceBotDataSet == null) {
                                        logger.debug("BotServiceAtom can't be retrieved, creating a new one...");
                                        createServiceAtom();
                                    } else if (!Objects.equals(new ServiceAtomModelWrapper(serviceBotDataSet)
                                                    .getServiceAtomContent(), serviceAtomContent)) {
                                        logger.debug("BotServiceAtom is outdated, modifying the BotServiceAtom");
                                        modifyServiceAtom();
                                    } else {
                                        logger.info("#####################################################################################");
                                        logger.info("BotServiceAtom is still up to date, atom URI is {}",
                                                        serviceAtomUri);
                                        logger.info("#####################################################################################");
                                    }
                                }
                            }

                            private void createServiceAtom() {
                                final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
                                WonNodeInformationService wonNodeInformationService = ctx
                                                .getWonNodeInformationService();
                                final URI atomUri = wonNodeInformationService.generateAtomURI(wonNodeUri);
                                Dataset botServiceDataset = new ServiceAtomModelWrapper(atomUri,
                                                serviceAtomContent).copyDataset();
                                logger.debug("creating BotServiceAtom on won node {} with content: {} ", wonNodeUri,
                                                RdfUtils.toString(botServiceDataset));
                                WonMessage createAtomMessage = createWonMessage(atomUri, botServiceDataset);
                                EventBotActionUtils.rememberInList(ctx, atomUri, uriListName);
                                EventBus bus = ctx.getEventBus();
                                EventListener successCallback = new EventListener() {
                                    @Override
                                    public void onEvent(Event event) {
                                        logger.info("#####################################################################################");
                                        logger.info("BotServiceAtom creation successful, new atom URI is {}",
                                                        atomUri);
                                        logger.info("#####################################################################################");
                                        serviceAtomContext.setServiceAtomUri(atomUri);
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
                            }

                            private void modifyServiceAtom() {
                                logger.info("BotServiceAtom modification currently not implemented");
                                // TODO: Implement BotServiceAtom modification
                            }
                        }));
        subscribeWithAutoCleanup(AtomCreatedEvent.class,
                        new ActionOnEventListener(ctx, new BaseEventBotAction(ctx) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                if (event instanceof AtomCreatedEvent) {
                                    AtomCreatedEvent atomCreatedEvent = (AtomCreatedEvent) event;
                                    URI botServiceAtomUri = serviceAtomContext.getServiceAtomUri();
                                    URI createdAtomUri = atomCreatedEvent.getAtomURI();
                                    if (!Objects.equals(createdAtomUri, botServiceAtomUri)) {
                                        if (ctx.getBotContext().isAtomKnown(createdAtomUri)) {
                                            logger.debug("Atom ({}) is known, must be one we created..., dataset: {}",
                                                            createdAtomUri,
                                                            RdfUtils.toString(atomCreatedEvent.getAtomDataset()));
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
                                                                                botServiceAtomUri, remoteWonNode,
                                                                                "Automated Connect to Service Atom")
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
        subscribeWithAutoCleanup(ConnectFromOtherAtomEvent.class,
                        new ActionOnEventListener(ctx, new BaseEventBotAction(ctx) {
                            @Override
                            protected void doRun(Event event, EventListener executingListener) throws Exception {
                                if (event instanceof ConnectFromOtherAtomEvent) {
                                    ConnectFromOtherAtomEvent connectFromOtherAtomEvent = (ConnectFromOtherAtomEvent) event;
                                    URI botServiceAtomUri = serviceAtomContext.getServiceAtomUri();
                                    URI senderAtomUri = connectFromOtherAtomEvent.getWonMessage().getSenderAtomURI();
                                    URI targetAtomUri = connectFromOtherAtomEvent.getWonMessage().getRecipientAtomURI();
                                    logger.debug("Possibly accept the connection Request with holderFacet<->holdableFacet if atom is BotServiceAtom");
                                    if (ctx.getBotContext().isAtomKnown(senderAtomUri)
                                                    && ctx.getBotContext().isAtomKnown(targetAtomUri)
                                                    && Objects.equals(targetAtomUri, botServiceAtomUri)) {
                                        logger.debug("Both Atoms belong to you, you might want to accept the connect, socketUri: {}, targetSocketUri: {}",
                                                        connectFromOtherAtomEvent.getWonMessage().getSenderSocketURI(),
                                                        connectFromOtherAtomEvent.getWonMessage()
                                                                        .getRecipientSocketURI());
                                        URI senderSocketUri = connectFromOtherAtomEvent.getWonMessage()
                                                        .getSenderSocketURI();
                                        URI targetSocketUri = connectFromOtherAtomEvent.getWonMessage()
                                                        .getRecipientSocketURI();
                                        Optional<URI> senderSocketTypeUri = WonLinkedDataUtils
                                                        .getTypeOfSocket(senderSocketUri, ctx.getLinkedDataSource());
                                        Optional<URI> targetSocketTypeUri = WonLinkedDataUtils
                                                        .getTypeOfSocket(targetSocketUri, ctx.getLinkedDataSource());
                                        if (senderSocketTypeUri.isPresent() && targetSocketTypeUri.isPresent()
                                                        && Objects.equals(senderSocketTypeUri.get(),
                                                                        URI.create(WXHOLD.HoldableSocketString))
                                                        && Objects.equals(targetSocketTypeUri.get(),
                                                                        URI.create(WXHOLD.HolderSocketString))) {
                                            logger.debug("Accepting connect request from atom ({}) to serviceAtom ({})",
                                                            senderAtomUri, targetAtomUri);
                                            URI serviceAtomWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(
                                                            ctx.getLinkedDataSource().getDataForResource(
                                                                            targetAtomUri),
                                                            targetAtomUri);
                                            WonMessage openServiceAtomMessage = WonMessageBuilder
                                                            .setMessagePropertiesForOpen(ctx
                                                                            .getWonNodeInformationService()
                                                                            .generateEventURI(serviceAtomWonNode),
                                                                            connectFromOtherAtomEvent.getWonMessage(),
                                                                            "Automated Open from Service Atom")
                                                            .build();
                                            ctx.getWonMessageSender().sendWonMessage(openServiceAtomMessage);
                                        }
                                    } else {
                                        logger.debug("At least one of the two Atoms is not known, or the targetAtomUri is not the botServiceAtomUri, ignore the connect request");
                                    }
                                }
                            }
                        }));
    }
}
