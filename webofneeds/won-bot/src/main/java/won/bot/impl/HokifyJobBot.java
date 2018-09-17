package won.bot.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.hokify.WonHokifyJobBotHandler;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyBotsApi;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyMessageGenerator;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.action.impl.telegram.receive.TelegramMessageReceivedAction;
import won.bot.framework.eventbot.action.impl.telegram.send.*;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramContentExtractor;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramMessageGenerator;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.CloseBevahiour;
import won.bot.framework.eventbot.behaviour.ConnectBehaviour;
import won.bot.framework.eventbot.behaviour.ConnectionMessageBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
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
    private String token;
    private String jsonURL;

    private EventBus bus;
    private WonHokifyJobBotHandler wonHokifyJobBotHandler;

    //@Autowired
    //private TelegramContentExtractor telegramContentExtractor;

    //@Autowired
    private HokifyMessageGenerator hokifyMessageGenerator;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        this.hokifyMessageGenerator = new HokifyMessageGenerator();
        this.hokifyMessageGenerator.setEventListenerContext(ctx);
        bus = getEventBus();

        //Initiate Telegram Bot Handler
        ApiContextInitializer.init();

        HokifyBotsApi hokifyBotsApi = new HokifyBotsApi(this.jsonURL);
        try {
            
            wonHokifyJobBotHandler = new WonHokifyJobBotHandler(bus, hokifyMessageGenerator, botName, token);
            logger.debug("botName: " + wonHokifyJobBotHandler.getBotUsername());
            logger.debug("botTokn: " + wonHokifyJobBotHandler.getBotToken());
            //hokifyBotsApi.registerBot(wonHokifyJobBotHandler);

            BotBehaviour connectBehaviour = new ConnectBehaviour(ctx);
            connectBehaviour.activate();

            BotBehaviour closeBehaviour = new CloseBevahiour(ctx);
            closeBehaviour.activate();

            BotBehaviour connectionMessageBehaviour = new ConnectionMessageBehaviour(ctx);
            connectionMessageBehaviour.activate();

            //Telegram initiated Events
            /*
            bus.subscribe(TelegramMessageReceivedEvent.class,
                    new ActionOnEventListener(
                            ctx,
                            "TelegramMessageReceived",
                            new TelegramMessageReceivedAction(ctx, wonTelegramBotHandler, telegramContentExtractor)
                    ));

            bus.subscribe(SendHelpEvent.class,
                    new ActionOnEventListener(
                            ctx,
                            "TelegramHelpAction",
                            new TelegramHelpAction(ctx, wonTelegramBotHandler)
                    ));

            bus.subscribe(TelegramCreateNeedEvent.class,
                    new ActionOnEventListener(
                            ctx,
                            "TelegramCreateAction",
                            new TelegramCreateAction(ctx, wonTelegramBotHandler, telegramContentExtractor)
                    ));

            //WON initiated Events
            bus.subscribe(HintFromMatcherEvent.class,
                    new ActionOnEventListener(
                            ctx,
                            "HintReceived",
                            new Hint2TelegramAction(ctx, wonTelegramBotHandler)
                    ));

            bus.subscribe(ConnectFromOtherNeedEvent.class,
                    new ActionOnEventListener(
                            ctx,
                            "ConnectReceived",
                            new Connect2TelegramAction(ctx, wonTelegramBotHandler)
                    ));

            bus.subscribe(MessageFromOtherNeedEvent.class,
                    new ActionOnEventListener(
                            ctx,
                            "ReceivedTextMessage",
                            new Message2TelegramAction(ctx, wonTelegramBotHandler)
                    ));*/

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void setBotName(final String botName) {
        this.botName = botName;
    }

    public void setToken(final String token) {
        this.token = token;
    }
    
    public void setJsonURL(final String jsonURL) {
        this.jsonURL = jsonURL;
    }

    public void setTelegramContentExtractor(TelegramContentExtractor telegramContentExtractor) {
        //this.telegramContentExtractor = telegramContentExtractor;
    }

    public void setTelegramMessageGenerator(TelegramMessageGenerator telegramMessageGenerator) {
        //this.telegramMessageGenerator = telegramMessageGenerator;
    }
}
