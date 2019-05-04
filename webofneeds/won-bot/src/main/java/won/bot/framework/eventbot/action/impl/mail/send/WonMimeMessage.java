package won.bot.framework.eventbot.action.impl.mail.send;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * Created by fsuda on 18.10.2016.
 */
public class WonMimeMessage extends MimeMessage {
    public WonMimeMessage(Session session) {
        super(session);
    }

    public WonMimeMessage(MimeMessage mimeMessage) throws MessagingException {
        super(mimeMessage);
    }

    public void setMessageId(String messageId) throws MessagingException {
        if (messageId.startsWith("<") && messageId.endsWith(">")) {
            this.setHeader("Message-ID", messageId);
        } else {
            this.setHeader("Message-ID", "<" + messageId + ">");
        }
    }

    public String getMessageIdHeader() throws MessagingException {
        return getHeader("Message-Id")[0];
    }

    @Override
    public void updateMessageID() throws MessagingException {
        if (getHeader("Message-Id") == null) {
            super.updateMessageID();
        }
    }
}
