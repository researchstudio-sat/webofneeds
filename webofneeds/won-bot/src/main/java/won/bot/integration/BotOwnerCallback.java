package won.bot.integration;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;

import won.bot.exception.NoBotResponsibleException;
import won.bot.framework.manager.BotManager;
import won.owner.protocol.message.OwnerCallback;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.model.Connection;

/**
 * OwnerProtocolOwnerServiceCallback that dispatches the calls to the bots.
 */
@Qualifier("default")
public class BotOwnerCallback implements OwnerCallback {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    BotManager botManager;
    TaskScheduler taskScheduler;

    @Override
    public void onCloseFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        taskScheduler.schedule(() -> {
            try {
                logger.debug("onCloseFromOtherAtom received for connection {}, message {} ", con.getConnectionURI(),
                                wonMessage.getMessageURI());
                botManager.getBotResponsibleForAtomUri(con.getAtomURI()).onCloseFromOtherAtom(con, wonMessage);
            } catch (NoBotResponsibleException e) {
                logger.debug("error while handling onCloseFromOtherAtom() message: {}", e.getMessage());
            } catch (Exception e) {
                logger.warn("error while handling onCloseFromOtherAtom()", e);
            }
        }, new Date());
    }

    @Override
    public void onAtomHintFromMatcher(final WonMessage wonMessage) {
        taskScheduler.schedule(() -> {
            if (wonMessage.getEnvelopeType() != WonMessageDirection.FROM_OWNER) {
                try {
                    botManager.getBotResponsibleForAtomUri(wonMessage.getRecipientAtomURI())
                                    .onAtomHintFromMatcher(wonMessage);
                } catch (NoBotResponsibleException e) {
                    logger.debug("error while handling onAtomHintFromMatcher() message: {}", e.getMessage());
                } catch (Exception e) {
                    logger.warn("error while handling onAtomHintFromMatcher()", e);
                }
            } else {
                logger.debug("Received echo for onAtomHintFromMatcher");
            }
        }, new Date());
    }

    @Override
    public void onSocketHintFromMatcher(final WonMessage wonMessage) {
        taskScheduler.schedule(() -> {
            if (wonMessage.getEnvelopeType() != WonMessageDirection.FROM_OWNER) {
                try {
                    botManager.getBotResponsibleForAtomUri(wonMessage.getRecipientAtomURI())
                                    .onSocketHintFromMatcher(wonMessage);
                } catch (NoBotResponsibleException e) {
                    logger.debug("error while handling onAtomHintFromMatcher() message: {}", e.getMessage());
                } catch (Exception e) {
                    logger.warn("error while handling onAtomHintFromMatcher()", e);
                }
            } else {
                logger.debug("Received echo for onAtomHintFromMatcher");
            }
        }, new Date());
    }

    @Override
    public void onConnectFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        taskScheduler.schedule(() -> {
            if (true) { // TODO: figure out how to detect echoes
                try {
                    logger.debug("onConnectFromOtherAtom called for connection {}, message {}",
                                    con.getConnectionURI(), wonMessage.getMessageURI());
                    botManager.getBotResponsibleForAtomUri(con.getAtomURI()).onConnectFromOtherAtom(con, wonMessage);
                } catch (NoBotResponsibleException e) {
                    logger.debug("error while handling onConnectFromOtherAtom() message: {}", e.getMessage());
                } catch (Exception e) {
                    logger.warn("error while handling onConnectFromOtherAtom()", e);
                }
            } else {
                logger.debug("Received echo for onConnectFromOtherAtom");
            }
        }, new Date());
    }

    @Override
    public void onMessageFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        taskScheduler.schedule(() -> {
            if (true) { // TODO: figure out how to detect echoes
                try {
                    logger.debug("onMessageFromOtherAtom for Connection {}, message {}", con.getConnectionURI(),
                                    wonMessage.getMessageURI());
                    botManager.getBotResponsibleForAtomUri(con.getAtomURI()).onMessageFromOtherAtom(con, wonMessage);
                } catch (NoBotResponsibleException e) {
                    logger.debug("error while handling onMessageFromOtherAtom() message: {}", e.getMessage());
                } catch (Exception e) {
                    logger.warn("error while handling onMessageFromOtherAtom()", e);
                }
            } else {
                logger.debug("Received echo for onMessageFromOtherAtom");
            }
        }, new Date());
    }

    @Override
    public void onSuccessResponse(final URI successfulMessageUri, final WonMessage wonMessage) {
        taskScheduler.schedule(() -> {
            try {
                logger.debug("onSuccessResponse for message {} ", successfulMessageUri);
                URI atomUri = wonMessage.getRecipientAtomURI();
                botManager.getBotResponsibleForAtomUri(atomUri).onSuccessResponse(successfulMessageUri, wonMessage);
            } catch (NoBotResponsibleException e) {
                logger.debug("error while handling onSuccessResponse() message: {}", e.getMessage());
            } catch (Exception e) {
                logger.warn("error while handling onSuccessResponse()", e);
            }
        }, new Date());
    }

    @Override
    public void onFailureResponse(final URI failedMessageUri, final WonMessage wonMessage) {
        taskScheduler.schedule(() -> {
            try {
                logger.debug("onFailureResponse for message {} ", failedMessageUri);
                URI atomUri = wonMessage.getRecipientAtomURI();
                botManager.getBotResponsibleForAtomUri(atomUri).onFailureResponse(failedMessageUri, wonMessage);
            } catch (NoBotResponsibleException e) {
                logger.debug("error while handling onFailureResponse() message: {}", e.getMessage());
            } catch (Exception e) {
                logger.warn("error while handling onFailureResponse()", e);
            }
        }, new Date());
    }

    public void setBotManager(BotManager botManager) {
        this.botManager = botManager;
    }

    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }
}
