package won.bot.impl;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.mail.receive.CreateNeedFromMailAction;
import won.bot.framework.eventbot.action.impl.mail.receive.MailCommandAction;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;
import won.bot.framework.eventbot.action.impl.mail.receive.MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.receive.SubscribeUnsubscribeAction;
import won.bot.framework.eventbot.action.impl.mail.send.Connect2MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.send.Hint2MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.send.Message2MailAction;
import won.bot.framework.eventbot.action.impl.mail.send.WelcomeMailAction;
import won.bot.framework.eventbot.action.impl.mail.send.WonMimeMessageGenerator;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.CloseBevahiour;
import won.bot.framework.eventbot.behaviour.ConnectBehaviour;
import won.bot.framework.eventbot.behaviour.ConnectionMessageBehaviour;
import won.bot.framework.eventbot.behaviour.DeactivateNeedBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.mail.CreateNeedFromMailEvent;
import won.bot.framework.eventbot.event.impl.mail.MailCommandEvent;
import won.bot.framework.eventbot.event.impl.mail.MailReceivedEvent;
import won.bot.framework.eventbot.event.impl.mail.SubscribeUnsubscribeEvent;
import won.bot.framework.eventbot.event.impl.mail.WelcomeMailEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * This Bot checks the E-Mails from a given set of configured E-Mail Adresses and creates Needs that represent these E-Mails
 * In Future Implementations it will support the bidirectional communication between the node content and the email sender?!
 * Created by fsuda on 27.09.2016.
 */
public class Mail2WonBot extends EventBot {
    @Autowired
    private MessageChannel receiveEmailChannel;

    @Autowired
    private MessageChannel sendEmailChannel;

    @Autowired
    MailContentExtractor mailContentExtractor;

    @Autowired
    WonMimeMessageGenerator mailGenerator;

    private EventBus bus;

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        mailGenerator.setEventListenerContext(ctx);

        bus = getEventBus();

        BotBehaviour connectBehaviour = new ConnectBehaviour(ctx);
        connectBehaviour.activate();

        BotBehaviour closeBehaviour = new CloseBevahiour(ctx);
        closeBehaviour.activate();

        BotBehaviour connectionMessageBehaviour = new ConnectionMessageBehaviour(ctx);
        connectionMessageBehaviour.activate();

        BotBehaviour deactivateNeedBehaviour = new DeactivateNeedBehaviour(ctx);
        deactivateNeedBehaviour.activate();

        //Mail initiated events
        bus.subscribe(MailReceivedEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "MailReceived",
                        new MailParserAction(ctx, mailContentExtractor)
                ));

        bus.subscribe(CreateNeedFromMailEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "CreateNeedFromMailEvent",
                        new CreateNeedFromMailAction(ctx, mailContentExtractor)

                ));

        bus.subscribe(WelcomeMailEvent.class, new ActionOnEventListener(
                ctx,
                "WelcomeMailAction",
                new WelcomeMailAction(mailGenerator, sendEmailChannel)

        ));

        bus.subscribe(MailCommandEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "MailCommandEvent",
                        new MailCommandAction(ctx, mailContentExtractor)
                ));


        bus.subscribe(SubscribeUnsubscribeEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "SubscribeUnsubscribeEvent",
                        new SubscribeUnsubscribeAction(ctx)
                ));

        //WON initiated Events
        bus.subscribe(HintFromMatcherEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "HintReceived",
                        new Hint2MailParserAction(mailGenerator, sendEmailChannel)
                ));

        bus.subscribe(ConnectFromOtherNeedEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "ConnectReceived",
                        new Connect2MailParserAction(mailGenerator, sendEmailChannel)
                ));

        bus.subscribe(MessageFromOtherNeedEvent.class,
                new ActionOnEventListener(
                        ctx,
                        "ReceivedTextMessage",
                        new Message2MailAction(mailGenerator, sendEmailChannel)
                ));
    }

    public void receive(MimeMessage message) {
        bus.publish(new MailReceivedEvent(message));
    }

    public void setReceiveEmailChannel(MessageChannel receiveEmailChannel) {
        this.receiveEmailChannel = receiveEmailChannel;
    }

    public void setSendEmailChannel(MessageChannel sendEmailChannel) {
        this.sendEmailChannel = sendEmailChannel;
    }

    public void setMailGenerator(WonMimeMessageGenerator mailGenerator) {
        this.mailGenerator = mailGenerator;
    }
}
