package won.bot.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.mail.send.Connect2MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.send.Hint2MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.send.Message2MailAction;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.action.impl.telegram.receive.TelegramMessageReceivedAction;
import won.bot.framework.eventbot.action.impl.telegram.send.*;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramContentExtractor;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramMessageGenerator;
import won.bot.framework.eventbot.action.impl.wonmessage.CloseConnectionUriAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionUriAction;
import won.bot.framework.eventbot.action.impl.wonmessage.SendMessageOnConnectionAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.command.SendTextMessageOnConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.CloseConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.OpenConnectionEvent;
import won.bot.framework.eventbot.event.impl.telegram.SendHelpEvent;
import won.bot.framework.eventbot.event.impl.telegram.TelegramCreateNeedEvent;
import won.bot.framework.eventbot.event.impl.telegram.TelegramMessageReceivedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * This Bot checks the Telegram-Messages sent to a given telegram-bot and creates Needs that represent the sent messages
 * Created by fsuda on 14.12.2016.
 */
public class Telegram2WonBot extends EventBot {
    private String botName;
    private String token;

    private EventBus bus;
    private WonTelegramBotHandler wonTelegramBotHandler;

    @Autowired
    private TelegramContentExtractor telegramContentExtractor;

    @Autowired
    private TelegramMessageGenerator telegramMessageGenerator;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        telegramMessageGenerator.setEventListenerContext(ctx);
        bus = getEventBus();

        //Initiate Telegram Bot Handler
        ApiContextInitializer.init();

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            wonTelegramBotHandler = new WonTelegramBotHandler(bus, telegramMessageGenerator, botName, token);
            logger.debug("botName: "+wonTelegramBotHandler.getBotUsername());
            logger.debug("botTokn: "+wonTelegramBotHandler.getBotToken());
            telegramBotsApi.registerBot(wonTelegramBotHandler);

            //Telegram initiated Events
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

            bus.subscribe(CloseConnectionEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "CloseCommandEvent",
                        new CloseConnectionUriAction(ctx)
                ));

            bus.subscribe(OpenConnectionEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "OpenCommandEvent",
                        new OpenConnectionUriAction(ctx)
                ));

            bus.subscribe(SendTextMessageOnConnectionEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "SendTextMessage",
                        new SendMessageOnConnectionAction(ctx)
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
                    ));

        } catch (TelegramApiRequestException e) {
            logger.error(e.getMessage());
        }
    }

    public void setBotName(final String botName) {
        this.botName = botName;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public void setTelegramContentExtractor(TelegramContentExtractor telegramContentExtractor) {
        this.telegramContentExtractor = telegramContentExtractor;
    }

    public void setTelegramMessageGenerator(TelegramMessageGenerator telegramMessageGenerator) {
        this.telegramMessageGenerator = telegramMessageGenerator;
    }
}
