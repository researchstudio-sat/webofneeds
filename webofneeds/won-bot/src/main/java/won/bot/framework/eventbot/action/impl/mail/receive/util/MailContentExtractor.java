package won.bot.framework.eventbot.action.impl.mail.receive.util;

public class MailContentExtractor {
    public static String getMessage(Object content) {
        String parsedMessage = content.toString().split("\\r")[0]; //TODO: MAKE THE MESSAGE EXTRACTION A LITTLE BIT MORE SUFISTICATED

        return parsedMessage;
    }
}
