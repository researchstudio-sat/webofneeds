package won.bot.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.bus.EventBus;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * This Bot checks the E-Mails from a given set of configured E-Mail Adresses and creates Needs that represent these E-Mails
 * In Future Implementations it will support the bidirectional communication between the node content and the email sender?!
 * Created by fsuda on 27.09.2016.
 */
public class Mail2WonBot extends EventBot{
    @Autowired
    MessageChannel receiveEmailChannel;

    @Override
    protected void initializeEventListeners() {
        EventBus bus = getEventBus();
    }

    public void receive(MimeMessage message) {
        try {
            logger.info("message received, subject: " + message.getSubject());
        }catch (MessagingException me){
            logger.error("i had a messaging exception");
            me.printStackTrace();
        }
    }

    public MessageChannel getReceiveEmailChannel() {
        return receiveEmailChannel;
    }

    public void setReceiveEmailChannel(MessageChannel receiveEmailChannel) {
        this.receiveEmailChannel = receiveEmailChannel;
    }
}
