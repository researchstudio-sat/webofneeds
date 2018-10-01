package won.bot.impl;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
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
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.CloseBevahiour;
import won.bot.framework.eventbot.behaviour.ConnectBehaviour;
import won.bot.framework.eventbot.behaviour.ConnectionMessageBehaviour;
import won.bot.framework.eventbot.behaviour.DeactivateNeedBehaviour;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.hokify.CreateNeedFromJobEvent;
import won.bot.framework.eventbot.event.impl.mail.CreateNeedFromMailEvent;
import won.bot.framework.eventbot.event.impl.telegram.SendHelpEvent;
import won.bot.framework.eventbot.event.impl.telegram.TelegramCreateNeedEvent;
import won.bot.framework.eventbot.event.impl.telegram.TelegramMessageReceivedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * This Bot checks the Hokify jobs and creates and publishes them as needs
 * created by MS on 17.09.2018
 */
public class HokifyJobBot extends EventBot {
    private String botName;
    //private String token;
    private String jsonURL;

    private EventBus bus;
    //private WonHokifyJobBotHandler wonHokifyJobBotHandler;

    // @Autowired
    private HokifyMessageGenerator hokifyMessageGenerator;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        this.hokifyMessageGenerator = new HokifyMessageGenerator();
        this.hokifyMessageGenerator.setEventListenerContext(ctx);
        bus = getEventBus();

        HokifyBotsApi hokifyBotsApi = new HokifyBotsApi(this.jsonURL);

        try {
            ArrayList<HokifyJob> hokifyJobs = hokifyBotsApi.fetchHokifyData();

            // wonHokifyJobBotHandler = new WonHokifyJobBotHandler(bus,
            // hokifyMessageGenerator, botName, token);

            bus = getEventBus();

            BotBehaviour executeWonMessageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
            executeWonMessageCommandBehaviour.activate();

            bus.subscribe(CreateNeedFromJobEvent.class,
                    new ActionOnEventListener(ctx, "CreateNeedFromJobEvent", new CreateNeedFromJobAction(ctx)

                    ));
            bus.publish(new CreateNeedFromJobEvent(hokifyJobs));

            
            /*
             * bus.subscribe(SendHelpEvent.class, new ActionOnEventListener( ctx,
             * "HokifyHelpAction", new HokifyHelpAction(ctx, wonHokifyJobBotHandler) ));
             */
            // WON initiated Events

            /*
            bus.subscribe(HintFromMatcherEvent.class,
                    new ActionOnEventListener(ctx, "HintReceived", new Hint2HokifyAction(ctx)));
            */
            bus.subscribe(ConnectFromOtherNeedEvent.class, new ActionOnEventListener(ctx, "ConnectReceived",
                    new Connect2HokifyAction(ctx)));

            bus.subscribe(MessageFromOtherNeedEvent.class, new ActionOnEventListener(ctx, "ReceivedTextMessage",
                    new Message2HokifyAction(ctx)));

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

}
