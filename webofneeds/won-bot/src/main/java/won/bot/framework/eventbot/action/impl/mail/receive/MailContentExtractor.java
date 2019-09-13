package won.bot.framework.eventbot.action.impl.mail.receive;

import won.bot.framework.eventbot.action.impl.mail.model.ActionType;
import won.bot.framework.eventbot.action.impl.mail.model.MailPropertyType;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts atom properties like title, description, atom type, etc from mails.
 * Configurable via regular expression patterns in spring xml
 */
public class MailContentExtractor {
    // check if mail is of type demand/supply/doTogether/critique?
    private Pattern demandTypePattern;
    private Pattern supplyTypePattern;
    private Pattern doTogetherTypePattern;
    private Pattern critiqueTypePattern;
    // extract array of tags from the content
    private Pattern tagExtractionPattern;
    // extract text message from the mail (e.g. excluding the quoted parts of
    // earlier mails)
    private Pattern textMessageExtractionPattern;
    // extract title from mail subject
    private Pattern titleExtractionPattern;
    // extract description from mail content
    private Pattern descriptionExtractionPattern;
    // check if the atom created from the mail should is used for testing only
    private Pattern usedForTestingPattern;
    // check if the atom created from the mail should be not matched with other
    // atoms
    private Pattern doNotMatchPattern;
    // check if this is a command mail of different action types
    private Pattern cmdClosePattern;
    private Pattern cmdConnectPattern;
    private Pattern cmdSubscribePattern;
    private Pattern cmdUnsubscribePattern;
    private Pattern cmdTakenPattern;

    public static String getMailReference(MimeMessage message) throws MessagingException {
        // first search the mail reference is in the in-reply-to header in case user
        // answered a mail
        // e.g. in case of messages, implicit connect
        String[] replyTo = message.getHeader("In-Reply-To");
        if (replyTo != null && replyTo.length > 0) {
            return replyTo[0];
        }
        // second search the mail reference in the subject written by predefined mailto
        // links
        // e.g. in case of close atom
        Pattern referenceMailPattern = Pattern.compile("Message-Id_(.+)");
        Matcher m = referenceMailPattern.matcher(message.getSubject());
        return m.find() ? m.group(1) : null;
    }

    public MailPropertyType getMailType(MimeMessage message) throws MessagingException {
        return getMailType(message.getSubject());
    }

    public MailPropertyType getMailType(String subject) {
        if (demandTypePattern.matcher(subject).matches()) {
            return MailPropertyType.DEMAND;
        } else if (supplyTypePattern.matcher(subject).matches()) {
            return MailPropertyType.OFFER;
        } else if (doTogetherTypePattern.matcher(subject).matches()) {
            return MailPropertyType.BOTH;
        } else if (critiqueTypePattern.matcher(subject).matches()) {
            return MailPropertyType.BOTH;
        }
        return null;
    }

    public static String getMailText(MimeMessage message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            return getMailTextFromMultiPart((MimeMultipart) message.getContent());
        }
        return null;
    }

    private static String getMailTextFromMultiPart(MimeMultipart mm) throws MessagingException, IOException {
        for (int i = 0; i < mm.getCount(); i++) {
            BodyPart part = mm.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                return part.getContent().toString();
            } else if (part.isMimeType("multipart/*")) {
                return getMailTextFromMultiPart((MimeMultipart) part.getContent());
            }
        }
        return null;
    }

    public static String getMailSender(MimeMessage message) throws MessagingException {
        Address[] froms = message.getFrom();
        return (froms == null ? null : ((InternetAddress) froms[0]).getAddress());
    }

    public void setDemandTypePattern(final Pattern demandTypePattern) {
        this.demandTypePattern = demandTypePattern;
    }

    public void setSupplyTypePattern(final Pattern supplyTypePattern) {
        this.supplyTypePattern = supplyTypePattern;
    }

    public void setDoTogetherTypePattern(final Pattern doTogetherTypePattern) {
        this.doTogetherTypePattern = doTogetherTypePattern;
    }

    public void setCritiqueTypePattern(final Pattern critiqueTypePattern) {
        this.critiqueTypePattern = critiqueTypePattern;
    }

    public void setTagExtractionPattern(final Pattern tagExtractionPattern) {
        this.tagExtractionPattern = tagExtractionPattern;
    }

    public void setTextMessageExtractionPattern(final Pattern textMessageExtractionPattern) {
        this.textMessageExtractionPattern = textMessageExtractionPattern;
    }

    public void setTitleExtractionPattern(final Pattern titleExtractionPattern) {
        this.titleExtractionPattern = titleExtractionPattern;
    }

    public void setDescriptionExtractionPattern(final Pattern descriptionExtractionPattern) {
        this.descriptionExtractionPattern = descriptionExtractionPattern;
    }

    public void setUsedForTestingPattern(Pattern usedForTestingPattern) {
        this.usedForTestingPattern = usedForTestingPattern;
    }

    public void setDoNotMatchPattern(Pattern doNotMatchPattern) {
        this.doNotMatchPattern = doNotMatchPattern;
    }

    public void setCmdClosePattern(final Pattern cmdClosePattern) {
        this.cmdClosePattern = cmdClosePattern;
    }

    public void setCmdConnectPattern(final Pattern cmdConnectPattern) {
        this.cmdConnectPattern = cmdConnectPattern;
    }

    public void setCmdSubscribePattern(final Pattern cmdSubscribePattern) {
        this.cmdSubscribePattern = cmdSubscribePattern;
    }

    public void setCmdUnsubscribePattern(final Pattern cmdUnsubscribePattern) {
        this.cmdUnsubscribePattern = cmdUnsubscribePattern;
    }

    public void setCmdTakenPattern(final Pattern cmdTakenPattern) {
        this.cmdTakenPattern = cmdTakenPattern;
    }

    public boolean isCreateAtomMail(MimeMessage message) throws MessagingException {
        return getMailType(message) != null;
    }

    public boolean isCommandMail(MimeMessage message) throws IOException, MessagingException {
        // command mail is either an answer mail (with reference) to a previous mail
        // (e.g. message, implicit connect) or an
        // explicitly set action command (e.g. subscribe, unsubscribe, close atom)
        return getMailReference(message) != null
                        || (getMailAction(message) != null && !ActionType.NO_ACTION.equals(getMailAction(message)));
    }

    public ActionType getMailAction(MimeMessage message) throws IOException, MessagingException {
        if (cmdSubscribePattern.matcher(message.getSubject()).matches()) {
            return ActionType.SUBSCRIBE;
        } else if (cmdUnsubscribePattern.matcher(message.getSubject()).matches()) {
            return ActionType.UNSUBSCRIBE;
        } else if (cmdClosePattern.matcher(message.getSubject()).matches()) {
            return ActionType.CLOSE_CONNECTION;
        } else if (cmdConnectPattern.matcher(message.getSubject()).matches()) {
            return ActionType.OPEN_CONNECTION;
        } else if (cmdTakenPattern.matcher(message.getSubject()).matches()) {
            return ActionType.CLOSE_ATOM;
        } else {
            return ActionType.NO_ACTION;
        }
    }

    public boolean isDoNotMatch(MimeMessage message) throws MessagingException {
        return doNotMatchPattern.matcher(message.getSubject()).matches();
    }

    public boolean isUsedForTesting(MimeMessage message) throws MessagingException {
        return usedForTestingPattern.matcher(message.getSubject()).matches();
    }

    public String getTitle(MimeMessage message) throws MessagingException {
        Matcher m = titleExtractionPattern.matcher(message.getSubject());
        return m.find() ? m.group() : null;
    }

    public String getDescription(MimeMessage message) throws MessagingException, IOException {
        String desc = getMailText(message);
        if (desc != null) {
            Matcher m = descriptionExtractionPattern.matcher(desc);
            if (m.find()) {
                return m.group().trim();
            }
        }
        return null;
    }

    public String getTextMessage(MimeMessage message) throws MessagingException, IOException {
        String textMessage = getMailText(message);
        if (textMessage != null) {
            Matcher m = textMessageExtractionPattern.matcher(textMessage);
            if (m.find()) {
                return m.group().trim();
            }
        }
        return null;
    }

    public String[] getTags(MimeMessage message) throws MessagingException, IOException {
        HashSet<String> tags = new HashSet<>();
        Matcher m = tagExtractionPattern.matcher(
                        message.getSubject() + " " + message.getContent());
        while (m.find()) {
            tags.add(m.group());
        }
        String[] tagArray = new String[tags.size()];
        tagArray = tags.toArray(tagArray);
        Arrays.sort(tagArray);
        return tagArray;
    }

    public static String getFromAddressString(MimeMessage message) throws MessagingException {
        Address[] froms = message.getFrom();
        return (froms == null) ? null : ((InternetAddress) froms[0]).getAddress();
    }
}
