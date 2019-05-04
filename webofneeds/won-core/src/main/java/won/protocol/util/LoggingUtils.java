package won.protocol.util;

import org.slf4j.Logger;

public class LoggingUtils {
    /**
     * Logs a message on loglevel INFO and the exception's stack trace on DEBUG. The
     * log message will contain the specified message plus the exception class and
     * the exception message.
     * 
     * @param logger a Log4j logger
     * @param message a String used as a log4j message. Use curly braces ('{}') as
     * placeholders for message params.
     * @param messageparams objects to be inserted where the placeholders are
     * @param e the exception
     */
    public static final void logMessageAsInfoAndStacktraceAsDebug(Logger logger, Exception e, String message,
                    Object... messageparams) {
        if (!logger.isInfoEnabled())
            return;
        String msg = message + " - exception: {}, message: {}";
        Object[] msgparams = new Object[messageparams.length + 2];
        System.arraycopy(messageparams, 0, msgparams, 0, messageparams.length);
        msgparams[msgparams.length - 2] = e.getClass();
        msgparams[msgparams.length - 1] = e.getMessage();
        logger.info(msg, msgparams);
        if (!logger.isDebugEnabled()) {
            logger.info("Stacktrace of cause is printed at loglevel 'debug'");
            return;
        }
        logger.debug("Stacktrace of cause: ", e);
    }
}
