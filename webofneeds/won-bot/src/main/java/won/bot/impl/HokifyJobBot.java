package won.bot.impl;

import java.time.Duration;
import java.util.ArrayList;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.PublishEventAction;
import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;
import won.bot.framework.eventbot.action.impl.hokify.receive.CreateAtomFromJobAction;
import won.bot.framework.eventbot.action.impl.hokify.send.Connect2HokifyAction;
import won.bot.framework.eventbot.action.impl.hokify.send.Message2HokifyAction;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyBotsApi;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyMessageGenerator;
import won.bot.framework.eventbot.action.impl.trigger.ActionOnTriggerEventListener;
import won.bot.framework.eventbot.action.impl.trigger.BotTrigger;
import won.bot.framework.eventbot.action.impl.trigger.BotTriggerEvent;
import won.bot.framework.eventbot.action.impl.trigger.StartBotTriggerCommandEvent;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.hokify.CreateAtomFromJobEvent;
import won.bot.framework.eventbot.event.impl.hokify.FetchHokifyJobDataEvent;
import won.bot.framework.eventbot.event.impl.hokify.StartHokifyFetchEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;

/**
 * This Bot checks the Hokify jobs and creates and publishes them as atoms
 * created by MS on 17.09.2018
 */
public class HokifyJobBot extends EventBot {
    private String botName;
    private int updateTime;
    private String jsonURL;
    private String geoURL;
    private int publishTime;
    private boolean createAllInOne;
    private ArrayList<HokifyJob> hokifyJobsList;
    private EventBus bus;
    // private WonHokifyJobBotHandler wonHokifyJobBotHandler;
    // @Autowired
    private HokifyMessageGenerator hokifyMessageGenerator;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        this.hokifyMessageGenerator = new HokifyMessageGenerator();
        this.hokifyMessageGenerator.setEventListenerContext(ctx);
        bus = getEventBus();
        HokifyBotsApi hokifyBotsApi = new HokifyBotsApi(this.jsonURL, this.geoURL);
        hokifyJobsList = hokifyBotsApi.fetchHokifyData();
        logger.info("Register JobBot with update time {}", updateTime);
        try {
            bus = getEventBus();
            BotBehaviour executeWonMessageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
            executeWonMessageCommandBehaviour.activate();
            bus.subscribe(CreateAtomFromJobEvent.class, new ActionOnEventListener(ctx, "CreateAtomFromJobEvent",
                            new CreateAtomFromJobAction(ctx, this.createAllInOne)));
            // Create the atoms
            BotTrigger createHokifyJobBotTrigger = new BotTrigger(ctx, Duration.ofMinutes(publishTime));
            createHokifyJobBotTrigger.activate();
            bus.subscribe(StartHokifyFetchEvent.class, new ActionOnFirstEventListener(ctx,
                            new PublishEventAction(ctx, new StartBotTriggerCommandEvent(createHokifyJobBotTrigger))));
            bus.subscribe(BotTriggerEvent.class, new ActionOnTriggerEventListener(ctx, createHokifyJobBotTrigger,
                            new BaseEventBotAction(ctx) {
                                @Override
                                protected void doRun(Event event, EventListener executingListener) throws Exception {
                                    bus.publish(new CreateAtomFromJobEvent(hokifyJobsList, hokifyBotsApi));
                                }
                            }));
            // Get Hokify data
            BotTrigger fetchHokifyJobDataTrigger = new BotTrigger(ctx, Duration.ofMinutes(updateTime));
            fetchHokifyJobDataTrigger.activate();
            bus.subscribe(FetchHokifyJobDataEvent.class, new ActionOnFirstEventListener(ctx,
                            new PublishEventAction(ctx, new StartBotTriggerCommandEvent(fetchHokifyJobDataTrigger))));
            bus.subscribe(BotTriggerEvent.class, new ActionOnTriggerEventListener(ctx, fetchHokifyJobDataTrigger,
                            new BaseEventBotAction(ctx) {
                                @Override
                                protected void doRun(Event event, EventListener executingListener) throws Exception {
                                    logger.info("Update Hokify Job Data");
                                    hokifyJobsList = hokifyBotsApi.fetchHokifyData();
                                }
                            }));
            // WON initiated Events
            /*
             * When uncommenting this one day, remember to EventBotAction hint2HokifyAction
             * = new Hint2HokifyAction(ctx); bus.subscribe(AtomHintFromMatcherEvent.class,
             * new ActionOnEventListener(ctx, "AtomHintReceived", hint2HokifyAction));
             * bus.subscribe(SocketHintFromMatcherEvent.class, new
             * ActionOnEventListener(ctx, "AtomHintReceived", hint2HokifyAction));
             */
            bus.subscribe(ConnectFromOtherAtomEvent.class,
                            new ActionOnEventListener(ctx, "ConnectReceived", new Connect2HokifyAction(ctx)));
            bus.subscribe(MessageFromOtherAtomEvent.class,
                            new ActionOnEventListener(ctx, "ReceivedTextMessage", new Message2HokifyAction(ctx)));
            bus.publish(new StartHokifyFetchEvent());
            bus.publish(new FetchHokifyJobDataEvent());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public String getBotName() {
        return this.botName;
    }

    public void setBotName(final String botName) {
        this.botName = botName;
    }

    public void setJsonURL(final String jsonURL) {
        this.jsonURL = jsonURL;
    }

    public HokifyMessageGenerator getHokifyMessageGenerator() {
        return hokifyMessageGenerator;
    }

    public void setHokifyMessageGenerator(HokifyMessageGenerator hokifyMessageGenerator) {
        this.hokifyMessageGenerator = hokifyMessageGenerator;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public String getGeoURL() {
        return geoURL;
    }

    public void setGeoURL(String geoURL) {
        this.geoURL = geoURL;
    }

    public int getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(int publishTime) {
        this.publishTime = publishTime;
    }

    public boolean isCreateAllInOne() {
        return createAllInOne;
    }

    public void setCreateAllInOne(boolean createAllInOne) {
        this.createAllInOne = createAllInOne;
    }
}
