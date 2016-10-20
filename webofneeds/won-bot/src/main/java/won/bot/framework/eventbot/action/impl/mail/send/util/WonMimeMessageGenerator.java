package won.bot.framework.eventbot.action.impl.mail.send.util;

import won.bot.framework.eventbot.action.impl.mail.receive.util.MailContentExtractor;
import won.bot.framework.eventbot.action.impl.mail.send.WonMimeMessage;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.URI;

/**
 * This Class is used to generate all Mails that are going to be sent via the Mail2WonBot
 */
public class WonMimeMessageGenerator {
    public static WonMimeMessage createHintMail(MimeMessage msgToRespondTo, URI remoteNeedUri) throws MessagingException, IOException {
        String respondToMailAddress = MailContentExtractor.getFromAddressString(msgToRespondTo);
        MimeMessage answerMessage = (MimeMessage) msgToRespondTo.reply(false);

        StringBuilder mailText = new StringBuilder("We found a Match for you:\n");
        mailText.append("{");
        mailText.append(remoteNeedUri);
        mailText.append("}\n\n");

        mailText.append("Original Message from <");
        mailText.append(respondToMailAddress);
        mailText.append("> on ");
        mailText.append(msgToRespondTo.getSentDate());
        mailText.append(":\n");
        String originalContent = ">"+msgToRespondTo.getContent().toString().replaceAll("\\n","\n>");
        mailText.append(originalContent);
        answerMessage.setText(mailText.toString());

        WonMimeMessage wonAnswerMessage = new WonMimeMessage(answerMessage);
        wonAnswerMessage.updateMessageID();

        return wonAnswerMessage;
    }

    public static WonMimeMessage createConnectMail(MimeMessage msgToRespondTo, URI remoteNeedUri) throws MessagingException, IOException{
        String respondToMailAddress = MailContentExtractor.getFromAddressString(msgToRespondTo);
        MimeMessage answerMessage = (MimeMessage) msgToRespondTo.reply(false);

        StringBuilder mailText = new StringBuilder("Someone wants to connect with you:\n");
        mailText.append("{");
        mailText.append(remoteNeedUri);
        mailText.append("}\n\n");

        mailText.append("Original Message from <");
        mailText.append(respondToMailAddress);
        mailText.append("> on ");
        mailText.append(msgToRespondTo.getSentDate());
        mailText.append(":\n");
        String originalContent = ">"+msgToRespondTo.getContent().toString().replaceAll("\\n","\n>");
        mailText.append(originalContent);
        answerMessage.setText(mailText.toString());

        //We need to create an instance of our own MimeMessage Implementation in order to have the Unique Message Id set before sending
        WonMimeMessage wonAnswerMessage = new WonMimeMessage(answerMessage);
        wonAnswerMessage.updateMessageID();

        return wonAnswerMessage;
    }
}
