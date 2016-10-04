package won.bot.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.action.impl.mail.Hint2MailParserAction;
import won.bot.framework.eventbot.action.impl.mail.MailParserAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.mail.MailReceivedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
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

    @Autowired
    private MessageChannel receiveEmailChannel;

    @Autowired
    private MessageChannel sendEmailChannel;

    private EventBus bus;

    @Override
    protected void initializeEventListeners() {
        bus = getEventBus();

        bus.subscribe(MailReceivedEvent.class,
        new ActionOnEventListener(
                getEventListenerContext(),
                "MailReceived",
                new MailParserAction(getEventListenerContext(), NAME_NEEDS, URIMAILRELATIONS_NAME)
        ));

        bus.subscribe(HintFromMatcherEvent.class,
        new ActionOnEventListener(
                getEventListenerContext(),
                "HintReceived",
                new Hint2MailParserAction(getEventListenerContext(), NAME_NEEDS, URIMAILRELATIONS_NAME, sendEmailChannel)
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
