package won.bot.framework.eventbot.action.impl.mail.send;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.tdb.TDB;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;
import won.protocol.message.WonMessage;
import won.protocol.model.BasicNeedType;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.sparql.WonQueries;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;

/**
 * This Class is used to generate all Mails that are going to be sent via the Mail2WonBot
 */
public class WonMimeMessageGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WonMimeMessageGenerator.class);

    @Autowired
    private VelocityEngine velocityEngine;

    private int MAX_PREVIOUS_MESSAGES = 3;

    private String sentFrom;
    private String sentFromName;
    private EventListenerContext eventListenerContext;

    public void setSentFrom(final String sentFrom) {
        this.sentFrom = sentFrom;
    }

    public void setSentFromName(final String sentFromName) {
        this.sentFromName = sentFromName;
    }

    /**
     * Creates Response Message that is sent when a need tries to connect with another need
     */
    public WonMimeMessage createConnectMail(MimeMessage msgToRespondTo, URI remoteNeedUri) throws MessagingException, IOException {
        VelocityContext velocityContext = putDefaultContent(msgToRespondTo, remoteNeedUri);

        StringWriter writer = new StringWriter();

        velocityEngine.getTemplate("mail-templates/connect-mail.vm").merge(velocityContext, writer);
        return generateWonMimeMessage(msgToRespondTo, writer.toString(), remoteNeedUri);
    }

    public WonMimeMessage createHintMail(MimeMessage msgToRespondTo, URI remoteNeedUri) throws MessagingException, IOException {
        VelocityContext velocityContext = putDefaultContent(msgToRespondTo, remoteNeedUri);

        StringWriter writer = new StringWriter();

        velocityEngine.getTemplate("mail-templates/hint-mail.vm").merge(velocityContext, writer);
        return generateWonMimeMessage(msgToRespondTo, writer.toString(), remoteNeedUri);
    }

    public WonMimeMessage createMessageMail(MimeMessage msgToRespondTo, URI requesterId, URI remoteNeedUri, URI connectionUri) throws MessagingException, IOException {
        VelocityContext velocityContext = putDefaultContent(msgToRespondTo, remoteNeedUri);

        putMessages(velocityContext, connectionUri, requesterId);

        StringWriter writer = new StringWriter();

        velocityEngine.getTemplate("mail-templates/message-mail.vm").merge(velocityContext, writer);
        return generateWonMimeMessage(msgToRespondTo, writer.toString(), remoteNeedUri);
    }

    /**
     * Creates the DefaultContext and fills in all relevant Infos which are used in every Mail, in our Case this is the NeedInfo and the quoted Original Message
     * @param msgToRespondTo Message that the new Mail is in ResponseTo (to extract the mailtext)
     * @param remoteNeedUri To extract the corresponding Need Data
     * @return VelocityContext that has prefilled all the necessary Data
     * @throws IOException
     * @throws MessagingException
     */
    private VelocityContext putDefaultContent(MimeMessage msgToRespondTo, URI remoteNeedUri) throws IOException, MessagingException {
        VelocityContext velocityContext = new VelocityContext();

        putRemoteNeedInfo(velocityContext, remoteNeedUri);
        putQuotedMail(velocityContext, msgToRespondTo);

        return velocityContext;
    }

    private WonMimeMessage generateWonMimeMessage(MimeMessage msgToRespondTo, String mailBody, URI remoteNeedUri)
      throws MessagingException, UnsupportedEncodingException {
        Dataset remoteNeedRDF = eventListenerContext.getLinkedDataSource().getDataForResource(remoteNeedUri);

        MimeMessage answerMessage = (MimeMessage) msgToRespondTo.reply(false);
        answerMessage.setFrom(new InternetAddress(sentFrom, sentFromName));
        answerMessage.setText(mailBody);
        answerMessage.setSubject(answerMessage.getSubject() + " <-> ["+ BasicNeedType.fromURI(WonRdfUtils.NeedUtils.getBasicNeedType(remoteNeedRDF))+"] " +  WonRdfUtils.NeedUtils.getNeedTitle(remoteNeedRDF));

        //We need to create an instance of our own MimeMessage Implementation in order to have the Unique Message Id set before sending
        WonMimeMessage wonAnswerMessage = new WonMimeMessage(answerMessage);
        wonAnswerMessage.updateMessageID();

        return  wonAnswerMessage;
    }

    /**
     * Responsible for filling inc/remote-need-info.vm template
     * @param velocityContext
     * @param remoteNeedUri
     */
    private void putRemoteNeedInfo(VelocityContext velocityContext, URI remoteNeedUri) {
        Dataset remoteNeedRDF = eventListenerContext.getLinkedDataSource().getDataForResource(remoteNeedUri);

        velocityContext.put("remoteNeedTitle", WonRdfUtils.NeedUtils.getNeedTitle(remoteNeedRDF).replaceAll("\\n", "\n>"));
        velocityContext.put("remoteNeedDescription", WonRdfUtils.NeedUtils.getNeedDescription(remoteNeedRDF).replaceAll("\\n", "\n>"));

        List<String> tags = WonRdfUtils.NeedUtils.getTags(remoteNeedRDF);
        velocityContext.put("remoteNeedTags", tags.size() > 0 ? tags : null);
        velocityContext.put("remoteNeedUri", remoteNeedUri);
    }

    /**
     * Responsible for filling inc/quoted-mail.vm template
     * @param velocityContext
     * @param msgToRespondTo
     * @throws MessagingException
     * @throws IOException
     */
    private void putQuotedMail(VelocityContext velocityContext, MimeMessage msgToRespondTo)
            throws MessagingException, IOException {
        String respondToMailAddress = MailContentExtractor.getFromAddressString(msgToRespondTo);

        velocityContext.put("respondAddress", respondToMailAddress);
        velocityContext.put("sentDate", msgToRespondTo.getSentDate());

        String mailText = MailContentExtractor.getMailText(msgToRespondTo);
        if (mailText != null) {
            velocityContext.put("respondMessage", mailText.replaceAll("\\n", "\n>"));
        }
    }

    /**
     * Responsible for filling inc/previous-messages.vm
     * @param velocityContext
     * @param connectionUri
     * @param requesterUri
     */
    private void putMessages(VelocityContext velocityContext, URI connectionUri, URI requesterUri) throws MessagingException, IOException {
        logger.debug("getting the messages for connectionuri: {}", connectionUri);

        Dataset baseDataSet = eventListenerContext.getLinkedDataSource().getDataForResource(connectionUri);
        Dataset eventDataSet = eventListenerContext.getLinkedDataSource().getDataForResource(URI.create(connectionUri.toString()+"/events?deep=true"), requesterUri);

        RdfUtils.addDatasetToDataset(baseDataSet, eventDataSet);

        try {
            List<String> previousMessages = new ArrayList<>();

            Query query = QueryFactory.create(WonQueries.SPARQL_TEXTMESSAGES_BY_CONNECTION_ORDERED_BY_TIMESTAMP);
            QuerySolutionMap initialBinding = new QuerySolutionMap();

            QueryExecution qExec = QueryExecutionFactory.create(query, baseDataSet, initialBinding);

            qExec.getContext().set(TDB.symUnionDefaultGraph, true);
            ResultSet results = qExec.execSelect();

            long currentMessageCount = 0;

            boolean lastSource = false;
            String quote = "";

            if(results.hasNext()){
                QuerySolution soln = results.nextSolution();
                lastSource = isYourMessage(soln, requesterUri);
                velocityContext.put("message", buildMessageLine(soln, quote, requesterUri, true));
            };

            if(MAX_PREVIOUS_MESSAGES != 0) {
                List<String> messageBlock = new ArrayList<>();
                quote = ">";
                while (results.hasNext()) {
                    currentMessageCount++;

                    if (MAX_PREVIOUS_MESSAGES != -1 && currentMessageCount > MAX_PREVIOUS_MESSAGES) {
                        previousMessages.add(quote+">"+"[...]");
                        break;
                    }

                    QuerySolution soln = results.nextSolution();
                    boolean msgSource = isYourMessage(soln, requesterUri); //Determine the source of this message

                    if(msgSource != lastSource){
                        quote += ">";
                        Collections.reverse(messageBlock);
                        previousMessages.addAll(messageBlock);
                        messageBlock.clear();
                    }

                    String messageLine = buildMessageLine(soln, quote, requesterUri, lastSource != msgSource);
                    messageBlock.add(messageLine);

                    lastSource = msgSource;
                }
                qExec.close();

                velocityContext.put("previousMessages", previousMessages);
            }
        } catch (QueryParseException e) {
            logger.error("query parse exception {}", e);
        }
    }

    /**
     * Builds a valid messageLine
     * @param soln
     * @param quote String to indicate the quotationhierachy
     * @param requesterUri
     * @param addSource If we need to add the Source of the message
     * @return
     */
    private static String buildMessageLine(QuerySolution soln, String quote, URI requesterUri, boolean addSource) {
        StringBuilder messageLine = new StringBuilder(quote);

        if(addSource) {
            if (isYourMessage(soln, requesterUri)) {
                messageLine.append("You said: ");
            } else {
                messageLine.append("They said: ");
            }
        }

        String message = soln.get("msg").asLiteral().getString();
        messageLine.append(message);

        StringBuilder replacementSb = new StringBuilder("\n").append(quote).append("\t");

        return messageLine.toString().replaceAll("\\n", replacementSb.toString());
    }

    /**
     * Determines the Source of the Message
     * @param soln
     * @param requesterUri
     * @return true if the message came from you, false if the message did not come from you
     */
    private static boolean isYourMessage(QuerySolution soln, URI requesterUri) {
        return requesterUri.toString().equals(soln.get("needUri").asResource().getURI());
    }

    private static String extractTextMessageFromWonMessage(WonMessage wonMessage){
        if (wonMessage == null) return null;
        String message = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        return StringUtils.trim(message);
    }

    public void setVelocityEngine(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    public void setMAX_PREVIOUS_MESSAGES(int MAX_PREVIOUS_MESSAGES) {
        this.MAX_PREVIOUS_MESSAGES = MAX_PREVIOUS_MESSAGES;
    }

    public void setEventListenerContext(EventListenerContext eventListenerContext) {
        this.eventListenerContext = eventListenerContext;
    }

    public EventListenerContext getEventListenerContext() {
        return eventListenerContext;
    }
}
