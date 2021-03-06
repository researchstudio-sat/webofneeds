package won.owner.web;

import org.apache.jena.query.Dataset;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import won.owner.model.EmailVerificationToken;
import won.owner.model.User;
import won.owner.service.impl.URIService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXGROUP;
import won.utils.mail.WonMailSender;

import java.io.File;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * User: ypanchenko Date: 23.02.2015
 */
public class WonOwnerMailSender {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String OWNER_TARGET_ATOM_LINK = "/#!/post?postUri=";
    private static final String OWNER_CONNECTION_LINK = "/#!/connections?requireLogin=true&postUri=%s&connectionUri=%s";
    private static final String OWNER_LOCAL_ATOM_LINK = "/#!/post?requireLogin=true&postUri=";
    private static final String OWNER_VERIFICATION_LINK = "/#!/inventory?token=";
    private static final String OWNER_ANONYMOUS_LINK = "/#!/inventory?privateId=";
    private static final String EXPORT_FILE_NAME = "export.zip";
    private static final String SUBJECT_CONVERSATION_MESSAGE = "New message";
    private static final String SUBJECT_CONNECT_CHAT = "New conversation request";
    private static final String SUBJECT_CONNECT = "New connect request";
    private static final String SUBJECT_MATCH = "New match";
    private static final String SUBJECT_MULTI_MATCH = " New matches";
    private static final String SUBJECT_CLOSE_CHAT = "Conversation closed";
    private static final String SUBJECT_CLOSE = "Connection closed";
    private static final String SUBJECT_SYSTEM_CLOSE = "Connection closed by system";
    private static final String SUBJECT_ATOM_MESSAGE = "Notification from WoN node";
    private static final String SUBJECT_SYSTEM_DEACTIVATE = "Posting deactivated by system";
    private static final String SUBJECT_VERIFICATION = "Please verify your email address";
    private static final String SUBJECT_PASSWORD_CHANGED = "Password changed";
    private static final String SUBJECT_ANONYMOUSLINK = "Anonymous login link";
    private static final String SUBJECT_EXPORT = "Your account export is complete";
    private static final String SUBJECT_EXPORT_FAILED = "Your account export did not succeed";
    private static final String SUBJECT_RECOVERY_KEY_GENERATED = "Your new recovery key";
    @Autowired
    LinkedDataSource linkedDataSource;
    private WonMailSender wonMailSender;
    @Value(value = "${uri.prefix}")
    private URI ownerWebappUri;
    @Autowired
    private URIService uriService;
    private VelocityEngine velocityEngine;
    private Template conversationNotificationTemplate;
    private Template connectNotificationTemplate;
    private Template closeNotificationTemplate;
    private Template systemCloseNotificationTemplate;
    private Template hintNotificationTemplate;
    private Template multipleHintsNotificationTemplate;
    private Template atomMessageNotificationTemplate;
    private Template systemDeactivateNotificationTemplate;
    private Template verificationTemplate;
    private Template anonymousTemplate;
    private Template exportTemplate;
    private Template exportFailedTemplate;
    private Template passwordChangedTemplate;
    private Template recoveryKeyGeneratedTemplate;

    public WonOwnerMailSender() {
        velocityEngine = new VelocityEngine();
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "file");
        properties.setProperty("file.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        // force velocity to log to tomcats standard log
        // as per https://stackoverflow.com/questions/849710/error-in-velocity-and-log4j
        properties.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
        properties.setProperty("runtime.log.logsystem.log4j.category", "velocity");
        properties.setProperty("runtime.log.logsystem.log4j.logger", "velocity");
        velocityEngine.init(properties);
        conversationNotificationTemplate = velocityEngine.getTemplate("mail-templates/conversation-notification.vm");
        connectNotificationTemplate = velocityEngine.getTemplate("mail-templates/connect-notification.vm");
        closeNotificationTemplate = velocityEngine.getTemplate("mail-templates/close-notification.vm");
        systemCloseNotificationTemplate = velocityEngine.getTemplate("mail-templates/systemclose-notification.vm");
        hintNotificationTemplate = velocityEngine.getTemplate("mail-templates/hint-notification.vm");
        multipleHintsNotificationTemplate = velocityEngine.getTemplate("mail-templates/multiple-hints-notification.vm");
        atomMessageNotificationTemplate = velocityEngine.getTemplate("mail-templates/atommessage-notification.vm");
        systemDeactivateNotificationTemplate = velocityEngine
                        .getTemplate("mail-templates/system-deactivate-notification.vm");
        verificationTemplate = velocityEngine.getTemplate("mail-templates/verification.vm");
        anonymousTemplate = velocityEngine.getTemplate("mail-templates/anonymous.vm");
        exportTemplate = velocityEngine.getTemplate("mail-templates/export.vm");
        exportFailedTemplate = velocityEngine.getTemplate("mail-templates/export-failed.vm");
        passwordChangedTemplate = velocityEngine.getTemplate("mail-templates/password-changed.vm");
        recoveryKeyGeneratedTemplate = velocityEngine.getTemplate("mail-templates/recovery-key-generated.vm");
    }

    public void setWonMailSender(WonMailSender wonMailSender) {
        this.wonMailSender = wonMailSender;
    }

    private String useValueOrDefaultValue(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private VelocityContext createContext(String toEmail, String localAtom, String targetAtom, String localConnection,
                    String localSocket, String targetSocket,
                    String textMsg) {
        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        if (targetAtom != null) {
            Dataset atomDataset = linkedDataSource.getDataForPublicResource(URI.create(targetAtom));
            DefaultAtomModelWrapper targetAtomWrapper = new DefaultAtomModelWrapper(atomDataset);
            velocityContext.put("targetAtomTitle", getLabelForAtomInMail(targetAtomWrapper));
            velocityContext.put("targetAtomIsPersona", isPersonaAtom(targetAtomWrapper));
            velocityContext.put("targetSocketIsGroupSocket", isGroupChatSocket(targetAtomWrapper, targetSocket));
            velocityContext.put("targetSocketIsChatSocket", isChatSocket(targetAtomWrapper, targetSocket));
            String linkTargetAtom = uriService.getOwnerProtocolOwnerURI() + OWNER_TARGET_ATOM_LINK + targetAtom;
            velocityContext.put("linkTargetAtom", linkTargetAtom);
        }
        if (localAtom != null) {
            Dataset localAtomDataset = linkedDataSource.getDataForPublicResource(URI.create(localAtom));
            DefaultAtomModelWrapper localAtomWrapper = new DefaultAtomModelWrapper(localAtomDataset);
            String linkLocalAtom = ownerAppLink + OWNER_LOCAL_ATOM_LINK + localAtom;
            velocityContext.put("linkLocalAtom", linkLocalAtom);
            velocityContext.put("localAtomTitle", getLabelForAtomInMail(localAtomWrapper));
            velocityContext.put("localAtomIsPersona", isPersonaAtom(localAtomWrapper));
            velocityContext.put("localSocketIsGroupSocket", isGroupChatSocket(localAtomWrapper, localSocket));
            velocityContext.put("localSocketIsChatSocket", isChatSocket(localAtomWrapper, localSocket));
        }
        if (localConnection != null && localAtom != null) {
            String linkConnection = ownerAppLink + String.format(OWNER_CONNECTION_LINK, localAtom, localConnection);
            velocityContext.put("linkConnection", linkConnection);
        }
        if (textMsg != null) {
            velocityContext.put("textMsg", textMsg);
        }
        if (this.ownerWebappUri != null) {
            velocityContext.put("serviceName", this.ownerWebappUri);
        }
        return velocityContext;
    }

    private boolean isGroupChatSocket(DefaultAtomModelWrapper atomWrapper, String socket) {
        if (socket == null) {
            return false;
        }
        return atomWrapper.getSocketTypeUri(socket)
                        .map(u -> u.equals(WXGROUP.GroupSocket.asURI()))
                        .orElse(false);
    }

    private boolean isChatSocket(DefaultAtomModelWrapper atomWrapper, String socket) {
        if (socket == null) {
            return false;
        }
        return atomWrapper.getSocketTypeUri(socket)
                        .map(su -> su.equals(WXCHAT.ChatSocket.asURI()))
                        .orElse(false);
    }

    private String getLabelForAtomInMail(DefaultAtomModelWrapper localAtomWrapper) {
        String label;
        label = localAtomWrapper.getSomeTitleFromIsOrAll("en", "de");
        if (label != null) {
            return label;
        }
        label = localAtomWrapper.getSomeName("en", "de");
        if (label != null) {
            return label;
        }
        return null;
    }

    private boolean isPersonaAtom(DefaultAtomModelWrapper atomModelWrapper) {
        Collection<URI> types = atomModelWrapper.getContentTypes();
        if (types != null) {
            return types.contains(URI.create(WON.Persona.getURI()));
        }
        return false;
    }

    private VelocityContext createVerificationContext(EmailVerificationToken verificationToken) {
        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        String verificationLinkUrl = ownerAppLink + OWNER_VERIFICATION_LINK + verificationToken.getToken();
        velocityContext.put("verificationLinkUrl", verificationLinkUrl);
        velocityContext.put("expirationDate", verificationToken.getExpiryDate());
        velocityContext.put("gracePeriodInHours", User.GRACEPERIOD_INHOURS);
        velocityContext.put("serviceName", this.ownerWebappUri);
        return velocityContext;
    }

    private VelocityContext createServiceNameOnlyContext() {
        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("serviceName", this.ownerWebappUri);
        return velocityContext;
    }

    private VelocityContext createRecoveryKeyContext(String recoveryKey) {
        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("recoveryKey", recoveryKey);
        velocityContext.put("serviceName", this.ownerWebappUri);
        return velocityContext;
    }

    private VelocityContext createAnonymousLinkContext(String privateId) {
        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        String anonymousLinkUrl = ownerAppLink + OWNER_ANONYMOUS_LINK + privateId;
        velocityContext.put("anonymousLinkUrl", anonymousLinkUrl);
        velocityContext.put("serviceName", this.ownerWebappUri);
        return velocityContext;
    }

    private VelocityContext createMultipleHintsContext(Map<String, Long> hintCountPerAtom) {
        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("hintCount", hintCountPerAtom);
        velocityContext.put("serviceName", this.ownerWebappUri);
        velocityContext.put("atoms", hintCountPerAtom.keySet());
        Map<String, String> atomLinks = new HashMap<>();
        Map<String, String> atomTitles = new HashMap<>();
        Map<String, Boolean> atomIsPersonas = new HashMap<>();
        hintCountPerAtom.keySet().stream().forEach(localAtom -> {
            Dataset localAtomDataset = linkedDataSource.getDataForPublicResource(URI.create(localAtom));
            DefaultAtomModelWrapper localAtomWrapper = new DefaultAtomModelWrapper(localAtomDataset);
            String linkLocalAtom = ownerAppLink + OWNER_LOCAL_ATOM_LINK + localAtom;
            atomLinks.put(localAtom, linkLocalAtom);
            atomTitles.put(localAtom, getLabelForAtomInMail(localAtomWrapper));
            atomIsPersonas.put(localAtom, isPersonaAtom(localAtomWrapper));
        });
        velocityContext.put("atomTitle", atomTitles);
        velocityContext.put("atomLink", atomLinks);
        velocityContext.put("atomIsPersona", atomIsPersonas);
        return velocityContext;
    }

    public void sendConversationNotificationMessage(String toEmail, String localAtom, String targetAtom,
                    String localConnection, String localSocket, String targetSocket, String textMsg) {
        if (textMsg != null && !textMsg.isEmpty()) {
            StringWriter writer = new StringWriter();
            VelocityContext context = createContext(toEmail, localAtom, targetAtom, localConnection, localSocket,
                            targetSocket, textMsg);
            conversationNotificationTemplate.merge(context, writer);
            logger.debug("sending " + SUBJECT_CONVERSATION_MESSAGE + " to " + toEmail);
            this.wonMailSender.sendTextMessage(toEmail, SUBJECT_CONVERSATION_MESSAGE, writer.toString());
        } else {
            logger.warn("do not send notification conversation email to {} with empty message. Connection is: {}",
                            toEmail, localConnection);
        }
    }

    public void sendConnectNotificationMessage(String toEmail, String localAtom, String targetAtom,
                    String localConnection, String localSocket, String targetSocket, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localAtom, targetAtom, localConnection, localSocket,
                        targetSocket, textMsg);
        connectNotificationTemplate.merge(context, writer);
        Dataset atomDataset = linkedDataSource.getDataForPublicResource(URI.create(localAtom));
        DefaultAtomModelWrapper wrapper = new DefaultAtomModelWrapper(atomDataset);
        String subject = SUBJECT_CONNECT;
        if (isChatSocket(wrapper, localSocket)) {
            subject = SUBJECT_CONNECT_CHAT;
        }
        logger.debug("sending {} to {}", subject, toEmail);
        this.wonMailSender.sendTextMessage(toEmail, subject, writer.toString());
    }

    public void sendCloseNotificationMessage(String toEmail, String localAtom, String targetAtom,
                    String localConnection, String localSocket, String targetSocket, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localAtom, targetAtom, localConnection, localSocket,
                        targetSocket, textMsg);
        closeNotificationTemplate.merge(context, writer);
        Dataset atomDataset = linkedDataSource.getDataForPublicResource(URI.create(localAtom));
        DefaultAtomModelWrapper wrapper = new DefaultAtomModelWrapper(atomDataset);
        String subject = SUBJECT_CLOSE;
        if (isChatSocket(wrapper, localSocket)) {
            subject = SUBJECT_CLOSE_CHAT;
        }
        logger.debug("sending {} to {}", subject, toEmail);
        this.wonMailSender.sendTextMessage(toEmail, subject, writer.toString());
    }

    public void sendHintNotificationMessage(String toEmail, String localAtom, String targetAtom,
                    String localConnection) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localAtom, targetAtom, localConnection, null, null, null);
        hintNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_MATCH + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_MATCH, writer.toString());
    }

    public void sendMultipleHintsNotificationMessage(String toEmail, Map<String, Long> hintCounts) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createMultipleHintsContext(hintCounts);
        multipleHintsNotificationTemplate.merge(context, writer);
        long hintCount = hintCounts.values().stream().collect(Collectors.summingLong(cnt -> cnt));
        logger.debug("sending " + hintCount + SUBJECT_MULTI_MATCH + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, hintCount + SUBJECT_MULTI_MATCH, writer.toString());
    }

    public void sendAtomMessageNotificationMessage(String toEmail, String localAtom, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localAtom, null, null, null, null, textMsg);
        atomMessageNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_ATOM_MESSAGE + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_ATOM_MESSAGE, writer.toString());
    }

    public void sendSystemDeactivateNotificationMessage(String toEmail, String localAtom, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localAtom, null, null, null, null, textMsg);
        systemDeactivateNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_SYSTEM_DEACTIVATE + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_SYSTEM_DEACTIVATE, writer.toString());
    }

    public void sendSystemCloseNotificationMessage(String toEmail, String localAtom, String targetAtom,
                    String localConnection, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localAtom, targetAtom, localConnection, null, null, textMsg);
        systemCloseNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_SYSTEM_CLOSE + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_SYSTEM_CLOSE, writer.toString());
    }

    public void sendVerificationMessage(User user, EmailVerificationToken verificationToken) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createVerificationContext(verificationToken);
        verificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_VERIFICATION + " to " + user.getEmail());
        this.wonMailSender.sendTextMessage(user.getEmail(), SUBJECT_VERIFICATION, writer.toString());
    }

    public void sendPasswordChangedMessage(User user) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createServiceNameOnlyContext();
        passwordChangedTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_PASSWORD_CHANGED + " to " + user.getEmail());
        this.wonMailSender.sendTextMessage(user.getEmail(), SUBJECT_PASSWORD_CHANGED, writer.toString());
    }

    public void sendAnonymousLinkMessage(String email, String privateId) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createAnonymousLinkContext(privateId);
        anonymousTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_ANONYMOUSLINK + " to " + email);
        this.wonMailSender.sendTextMessage(email, SUBJECT_ANONYMOUSLINK, writer.toString());
    }

    public void sendExportMessage(String email, File file) {
        StringWriter writer = new StringWriter();
        exportTemplate.merge(new VelocityContext(), writer);
        logger.debug("sending " + SUBJECT_EXPORT + " to " + email);
        this.wonMailSender.sendFileMessage(email, SUBJECT_EXPORT, writer.toString(), EXPORT_FILE_NAME, file);
    }

    public void sendExportFailedMessage(String email) {
        StringWriter writer = new StringWriter();
        exportFailedTemplate.merge(new VelocityContext(), writer);
        logger.debug("sending " + SUBJECT_EXPORT_FAILED + " to " + email);
        this.wonMailSender.sendHtmlMessage(email, SUBJECT_EXPORT_FAILED, writer.toString());
    }

    public void sendRecoveryKeyGeneratedMessage(User user, String recoveryKey) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createRecoveryKeyContext(recoveryKey);
        recoveryKeyGeneratedTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_RECOVERY_KEY_GENERATED + " to " + user.getEmail());
        this.wonMailSender.sendTextMessage(user.getEmail(), SUBJECT_RECOVERY_KEY_GENERATED, writer.toString());
    }
    /*
     * Dead main method code. Useful for trying out velocity stuff when needed.
     * public static void main(String... args){ VelocityEngine velocityEngine = new
     * VelocityEngine(); Properties properties = new Properties();
     * properties.setProperty("resource.loader", "file");
     * properties.setProperty("file.resource.loader.class",
     * "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
     * velocityEngine.init(properties); Template template =
     * velocityEngine.getTemplate("mail-templates/conversation-notification.vm");
     * StringWriter writer = new StringWriter(); VelocityContext context = new
     * VelocityContext(); EventCartridge ec = new EventCartridge();
     * ec.addEventHandler(new EscapeHtmlReference()); ec.attachToContext( context );
     * context.put("linkTargetAtom",
     * "https://satvm02.researchstudio.at/owner/#!/post/?postUri=https:%2F%2Fsatvm02.researchstudio.at%2Fwon%2Fresource%2Fatom%2F8772930375045372000"
     * ); context.put("targetAtomTitle", "höhöhö"); context.put("linkLocalAtom",
     * "https://satvm02.researchstudio.at/owner/#!/post/?postUri=https:%2F%2Fsatvm02.researchstudio.at%2Fwon%2Fresource%2Fatom%2F8772930375045372000"
     * ); context.put("localAtomTitle", "Ich & ich"); context.put("linkConnection",
     * "https://satvm02.researchstudio.at/owner/#!/post/?postUri=https:%2F%2Fsatvm02.researchstudio.at%2Fwon%2Fresource%2Fatom%2F8772930375045372000"
     * ); context.put("textMsg",
     * "Hä? & was soll das jetzt? <script language=\"JavaScript\"> alert('hi') </script>"
     * ); template.merge(context, writer); System.out.println(writer.toString()); }
     */
}
