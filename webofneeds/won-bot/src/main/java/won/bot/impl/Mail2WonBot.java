package won.bot.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.mail.receive.CreateNeedFromMailAction;
import won.bot.framework.eventbot.action.impl.mail.receive.MailCommandAction;
import won.bot.framework.eventbot.action.impl.mail.receive.MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.send.Connect2MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.send.Hint2MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.send.Message2MailAction;
import won.bot.framework.eventbot.action.impl.wonmessage.CloseConnectionUriAction;
import won.bot.framework.eventbot.action.impl.wonmessage.OpenConnectionUriAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.mail.*;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

import javax.mail.internet.MimeMessage;

/**
 * This Bot checks the E-Mails from a given set of configured E-Mail Adresses and creates Needs that represent these E-Mails
 * In Future Implementations it will support the bidirectional communication between the node content and the email sender?!
 * Created by fsuda on 27.09.2016.
 */
public class Mail2WonBot extends EventBot{
    private static final String NAME_NEEDS = "mailNeeds";
    private static final String URIMAILRELATIONS_NAME = "uriMailRelations";
    private static final String URIMIMEMESSAGERELATIONS_NAME = "uriMimeMessageRelations";
    private static final String MAILIDURIRELATIONS_NAME = "mailIdUriRelations";

    @Autowired
    private MessageChannel receiveEmailChannel;

    @Autowired
    private MessageChannel sendEmailChannel;

    private EventBus bus;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        bus = getEventBus();

        bus.subscribe(MailReceivedEvent.class,
        new ActionOnEventListener(
                ctx,
                "MailReceived",
                new MailParserAction(ctx)
        ));

        bus.subscribe(HintFromMatcherEvent.class,
        new ActionOnEventListener(
                ctx,
                "HintReceived",
                new Hint2MailParserAction(ctx, NAME_NEEDS, URIMAILRELATIONS_NAME, URIMIMEMESSAGERELATIONS_NAME, MAILIDURIRELATIONS_NAME, sendEmailChannel)
        ));

        bus.subscribe(ConnectFromOtherNeedEvent.class,
        new ActionOnEventListener(
                ctx,
                "ConnectReceived",
                new Connect2MailParserAction(ctx, NAME_NEEDS, URIMAILRELATIONS_NAME, URIMIMEMESSAGERELATIONS_NAME, MAILIDURIRELATIONS_NAME, sendEmailChannel)
        ));

        bus.subscribe(CreateNeedFromMailEvent.class,
        new ActionOnEventListener(
                ctx,
                "CreateNeedFromMailEvent",
                new CreateNeedFromMailAction(ctx, NAME_NEEDS, URIMAILRELATIONS_NAME, URIMIMEMESSAGERELATIONS_NAME)

        ));
        bus.subscribe(MailCommandEvent.class,
        new ActionOnEventListener(
                ctx,
                "MailCommandEvent",
                new MailCommandAction(ctx, MAILIDURIRELATIONS_NAME)
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

        //if the bot receives a text message - try to map the command of the text message to a DebugEvent
        bus.subscribe(MessageFromOtherNeedEvent.class,
        new ActionOnEventListener(
                ctx,
                "ReceivedTextMessage",
                new Message2MailAction(ctx, MAILIDURIRELATIONS_NAME, sendEmailChannel)
        ));
    }

    public void receive(MimeMessage message) {
        bus.publish(new MailReceivedEvent(message));
    }

    public MessageChannel getReceiveEmailChannel() {
        return receiveEmailChannel;
    }

    public void setReceiveEmailChannel(MessageChannel receiveEmailChannel) {
        this.receiveEmailChannel = receiveEmailChannel;
    }

    public MessageChannel getSendEmailChannel() {
        return sendEmailChannel;
    }

    public void setSendEmailChannel(MessageChannel sendEmailChannel) {
        this.sendEmailChannel = sendEmailChannel;
    }
}
