package won.bot.impl;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.action.impl.mail.receive.CreateAtomFromMailAction;
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
import won.bot.framework.eventbot.behaviour.DeactivateAtomBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.mail.CreateAtomFromMailEvent;
import won.bot.framework.eventbot.event.impl.mail.MailCommandEvent;
import won.bot.framework.eventbot.event.impl.mail.MailReceivedEvent;
import won.bot.framework.eventbot.event.impl.mail.SubscribeUnsubscribeEvent;
import won.bot.framework.eventbot.event.impl.mail.WelcomeMailEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * This Bot checks the E-Mails from a given set of configured E-Mail Adresses
 * and creates Atoms that represent these E-Mails In Future Implementations it
 * will support the bidirectional communication between the node content and the
 * email sender?! Created by fsuda on 27.09.2016.
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
        BotBehaviour deactivateAtomBehaviour = new DeactivateAtomBehaviour(ctx);
        deactivateAtomBehaviour.activate();
        // Mail initiated events
        bus.subscribe(MailReceivedEvent.class, new ActionOnEventListener(ctx, "MailReceived",
                        new MailParserAction(ctx, mailContentExtractor)));
        bus.subscribe(CreateAtomFromMailEvent.class, new ActionOnEventListener(ctx, "CreateAtomFromMailEvent",
                        new CreateAtomFromMailAction(ctx, mailContentExtractor)));
        bus.subscribe(WelcomeMailEvent.class, new ActionOnEventListener(ctx, "WelcomeMailAction",
                        new WelcomeMailAction(mailGenerator, sendEmailChannel)));
        bus.subscribe(MailCommandEvent.class, new ActionOnEventListener(ctx, "MailCommandEvent",
                        new MailCommandAction(ctx, mailContentExtractor)));
        bus.subscribe(SubscribeUnsubscribeEvent.class, new ActionOnEventListener(ctx, "SubscribeUnsubscribeEvent",
                        new SubscribeUnsubscribeAction(ctx)));
        // WON initiated Events
        EventBotAction hint2MailParserAction = new Hint2MailParserAction(mailGenerator, sendEmailChannel);
        bus.subscribe(AtomHintFromMatcherEvent.class,
                        new ActionOnEventListener(ctx, "AtomHintReceived", hint2MailParserAction));
        bus.subscribe(SocketHintFromMatcherEvent.class,
                        new ActionOnEventListener(ctx, "SocketHintReceived", hint2MailParserAction));
        bus.subscribe(ConnectFromOtherAtomEvent.class, new ActionOnEventListener(ctx, "ConnectReceived",
                        new Connect2MailParserAction(mailGenerator, sendEmailChannel)));
        bus.subscribe(MessageFromOtherAtomEvent.class, new ActionOnEventListener(ctx, "ReceivedTextMessage",
                        new Message2MailAction(mailGenerator, sendEmailChannel)));
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
