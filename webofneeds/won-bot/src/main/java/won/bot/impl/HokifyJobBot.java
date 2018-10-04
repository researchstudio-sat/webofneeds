package won.bot.impl;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.PublishEventAction;
import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;
import won.bot.framework.eventbot.action.impl.hokify.WonHokifyJobBotHandler;
import won.bot.framework.eventbot.action.impl.hokify.receive.CreateNeedFromJobAction;
import won.bot.framework.eventbot.action.impl.hokify.send.Connect2HokifyAction;
import won.bot.framework.eventbot.action.impl.hokify.send.Hint2HokifyAction;
import won.bot.framework.eventbot.action.impl.hokify.send.HokifyHelpAction;
import won.bot.framework.eventbot.action.impl.hokify.send.Message2HokifyAction;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyBotsApi;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyMessageGenerator;
import won.bot.framework.eventbot.action.impl.mail.receive.CreateNeedFromMailAction;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.action.impl.telegram.receive.TelegramMessageReceivedAction;
import won.bot.framework.eventbot.action.impl.telegram.send.*;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramContentExtractor;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramMessageGenerator;
import won.bot.framework.eventbot.action.impl.trigger.ActionOnTriggerEventListener;
import won.bot.framework.eventbot.action.impl.trigger.BotTrigger;
import won.bot.framework.eventbot.action.impl.trigger.BotTriggerEvent;
import won.bot.framework.eventbot.action.impl.trigger.StartBotTriggerCommandEvent;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.CloseBevahiour;
import won.bot.framework.eventbot.behaviour.ConnectBehaviour;
import won.bot.framework.eventbot.behaviour.ConnectionMessageBehaviour;
import won.bot.framework.eventbot.behaviour.DeactivateNeedBehaviour;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryNeedCreationSkippedEvent;
import won.bot.framework.eventbot.event.impl.factory.StartFactoryNeedCreationEvent;
import won.bot.framework.eventbot.event.impl.hokify.CreateNeedFromJobEvent;
import won.bot.framework.eventbot.event.impl.hokify.StartHokifyFetchEvent;
import won.bot.framework.eventbot.event.impl.mail.CreateNeedFromMailEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedProducerExhaustedEvent;
import won.bot.framework.eventbot.event.impl.telegram.SendHelpEvent;
import won.bot.framework.eventbot.event.impl.telegram.TelegramCreateNeedEvent;
import won.bot.framework.eventbot.event.impl.telegram.TelegramMessageReceivedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.protocol.util.WonRdfUtils;

/**
 * This Bot checks the Hokify jobs and creates and publishes them as needs
 * created by MS on 17.09.2018
 */
public class HokifyJobBot extends EventBot {
    private String botName;
    private int updateTime;
    private String jsonURL;
    private String geoURL;

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
        logger.info("Register JobBot with update time {}", updateTime);
        try {

            // wonHokifyJobBotHandler = new WonHokifyJobBotHandler(bus,
            // hokifyMessageGenerator, botName, token);

            bus = getEventBus();

            BotBehaviour executeWonMessageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
            executeWonMessageCommandBehaviour.activate();

            bus.subscribe(CreateNeedFromJobEvent.class, new ActionOnEventListener(ctx, "CreateNeedFromJobEvent",
                    new CreateNeedFromJobAction(ctx, hokifyBotsApi)));
            // bus.publish(new CreateNeedFromJobEvent(hokifyBotsApi));

            BotTrigger createHokifyJobBotTrigger = new BotTrigger(ctx, Duration.ofMinutes(updateTime));
            createHokifyJobBotTrigger.activate();
            bus.subscribe(StartHokifyFetchEvent.class, new ActionOnFirstEventListener(ctx,
                    new PublishEventAction(ctx, new StartBotTriggerCommandEvent(createHokifyJobBotTrigger))));
            bus.subscribe(BotTriggerEvent.class,
                    new ActionOnTriggerEventListener(ctx, createHokifyJobBotTrigger, new BaseEventBotAction(ctx) {
                        @Override
                        protected void doRun(Event event, EventListener executingListener) throws Exception {
                            bus.publish(new CreateNeedFromJobEvent(hokifyBotsApi));
                        }
                    }));

            // WON initiated Events

            /*
             * bus.subscribe(HintFromMatcherEvent.class, new ActionOnEventListener(ctx,
             * "HintReceived", new Hint2HokifyAction(ctx)));
             */
            bus.subscribe(ConnectFromOtherNeedEvent.class,
                    new ActionOnEventListener(ctx, "ConnectReceived", new Connect2HokifyAction(ctx)));

            bus.subscribe(MessageFromOtherNeedEvent.class,
                    new ActionOnEventListener(ctx, "ReceivedTextMessage", new Message2HokifyAction(ctx)));

            bus.publish(new StartHokifyFetchEvent());
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
    
    

}
