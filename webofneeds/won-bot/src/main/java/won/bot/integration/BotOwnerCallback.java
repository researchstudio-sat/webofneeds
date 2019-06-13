package won.bot.integration;

import java.net.URI;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;

import won.bot.framework.bot.Bot;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());
    BotManager botManager;
    TaskScheduler taskScheduler;

    @Override
    public void onCloseFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    logger.debug("onCloseFromOtherAtom received for connection {}, message {} ", con.getConnectionURI(),
                                    wonMessage.getMessageURI());
                    getBotForAtomUri(con.getAtomURI()).onCloseFromOtherAtom(con, wonMessage);
                } catch (Exception e) {
                    logger.warn("error while handling onCloseFromOtherAtom()", e);
                }
            }
        }, new Date());
    }

    @Override
    public void onAtomHintFromMatcher(final WonMessage wonMessage) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                if (wonMessage.getEnvelopeType() != WonMessageDirection.FROM_OWNER) {
                    try {
                        getBotForAtomUri(wonMessage.getRecipientAtomURI()).onAtomHintFromMatcher(wonMessage);
                    } catch (Exception e) {
                        logger.warn("error while handling onAtomHintFromMatcher()", e);
                    }
                } else {
                    logger.debug("Received echo for onAtomHintFromMatcher");
                }
            }
        }, new Date());
    }

    @Override
    public void onSocketHintFromMatcher(final WonMessage wonMessage) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                if (wonMessage.getEnvelopeType() != WonMessageDirection.FROM_OWNER) {
                    try {
                        getBotForAtomUri(wonMessage.getRecipientAtomURI()).onSocketHintFromMatcher(wonMessage);
                    } catch (Exception e) {
                        logger.warn("error while handling onAtomHintFromMatcher()", e);
                    }
                } else {
                    logger.debug("Received echo for onAtomHintFromMatcher");
                }
            }
        }, new Date());
    }

    @Override
    public void onConnectFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                if (wonMessage.getEnvelopeType() != WonMessageDirection.FROM_OWNER) {
                    try {
                        logger.debug("onConnectFromOtherAtom called for connection {}, message {}",
                                        con.getConnectionURI(), wonMessage.getMessageURI());
                        getBotForAtomUri(con.getAtomURI()).onConnectFromOtherAtom(con, wonMessage);
                    } catch (Exception e) {
                        logger.warn("error while handling onConnectFromOtherAtom()", e);
                    }
                } else {
                    logger.debug("Received echo for onConnectFromOtherAtom");
                }
            }
        }, new Date());
    }

    @Override
    public void onOpenFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                if (wonMessage.getEnvelopeType() != WonMessageDirection.FROM_OWNER) {
                    try {
                        getBotForAtomUri(con.getAtomURI()).onOpenFromOtherAtom(con, wonMessage);
                    } catch (Exception e) {
                        logger.warn("error while handling onOpenFromOtherAtom()", e);
                    }
                } else {
                    logger.debug("Received echo for onOpenFromOtherAtom");
                }
            }
        }, new Date());
    }

    @Override
    public void onMessageFromOtherAtom(final Connection con, final WonMessage wonMessage) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                if (wonMessage.getEnvelopeType() != WonMessageDirection.FROM_OWNER) {
                    try {
                        logger.debug("onMessageFromOtherAtom for Connection {}, message {}", con.getConnectionURI(),
                                        wonMessage.getMessageURI());
                        getBotForAtomUri(con.getAtomURI()).onMessageFromOtherAtom(con, wonMessage);
                    } catch (Exception e) {
                        logger.warn("error while handling onMessageFromOtherAtom()", e);
                    }
                } else {
                    logger.debug("Received echo for onMessageFromOtherAtom");
                }
            }
        }, new Date());
    }

    @Override
    public void onSuccessResponse(final URI successfulMessageUri, final WonMessage wonMessage) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    logger.debug("onSuccessResponse for message {} ", successfulMessageUri);
                    URI atomUri = wonMessage.getRecipientAtomURI();
                    getBotForAtomUri(atomUri).onSuccessResponse(successfulMessageUri, wonMessage);
                } catch (Exception e) {
                    logger.warn("error while handling onSuccessResponse()", e);
                }
            }
        }, new Date());
    }

    @Override
    public void onFailureResponse(final URI failedMessageUri, final WonMessage wonMessage) {
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    logger.debug("onFailureResponse for message {} ", failedMessageUri);
                    URI atomUri = wonMessage.getRecipientAtomURI();
                    getBotForAtomUri(atomUri).onFailureResponse(failedMessageUri, wonMessage);
                } catch (Exception e) {
                    logger.warn("error while handling onFailureResponse()", e);
                }
            }
        }, new Date());
    }

    public void setBotManager(BotManager botManager) {
        this.botManager = botManager;
    }

    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    private Bot getBotForAtomUri(URI atomUri) {
        Bot bot = botManager.getBotForAtomURI(atomUri);
        if (bot == null)
            throw new IllegalStateException("No bot registered for uri " + atomUri);
        if (!bot.getLifecyclePhase().isActive()) {
            throw new IllegalStateException("bot responsible for atom " + atomUri
                            + " is not active (lifecycle phase is: " + bot.getLifecyclePhase() + ")");
        }
        return bot;
    }
}
