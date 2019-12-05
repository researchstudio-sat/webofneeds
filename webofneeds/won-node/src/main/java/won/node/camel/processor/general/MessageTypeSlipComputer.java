package won.node.camel.processor.general;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import won.node.camel.service.WonCamelHelper;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;

/**
 * Computes a message slip for message processors that are annotated with
 * appropriate marker annotations. The annotation class to look for has to be
 * passed to this slip in the constructor.
 */
public class MessageTypeSlipComputer implements InitializingBean, ApplicationContextAware, Expression {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    HashMap<String, Object> fixedMessageProcessorsMap;
    private ApplicationContext applicationContext;
    private Class annotationClazz;
    private boolean allowNoMatchingProcessor = false;

    public MessageTypeSlipComputer(final String annotationClazzName) throws ClassNotFoundException {
        this.annotationClazz = Class.forName(annotationClazzName);
    }

    public MessageTypeSlipComputer(final String annotationClazzName, final boolean allowNoMatchingProcessor)
                    throws ClassNotFoundException {
        this(annotationClazzName);
        this.allowNoMatchingProcessor = allowNoMatchingProcessor;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        fixedMessageProcessorsMap = (HashMap) applicationContext.getBeansWithAnnotation(this.annotationClazz);
    }

    @Override
    public <T> T evaluate(final Exchange exchange, final Class<T> type) {
        WonMessage message = WonCamelHelper.getMessageRequired(exchange);
        String slip = "";
        // exchange.getIn().setHeader();
        URI messageType = WonCamelHelper.getMessageTypeRequired(exchange).getURI();
        URI direction = WonCamelHelper.getDirectionRequired(exchange).getURI();
        if (logger.isDebugEnabled()) {
            logger.debug("Received {}", message.toShortStringForDebug());
        }
        try {
            slip = computeMessageTypeSlip(messageType, direction);
            if (slip == null || slip.isEmpty()) {
                return null;
            }
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        }
        return type.cast(slip);
    }

    private String computeMessageTypeSlip(URI messageType, URI direction)
                    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Iterator iter = fixedMessageProcessorsMap.entrySet().iterator();
        StringBuilder slipBuilder = new StringBuilder();
        while (iter.hasNext()) {
            Map.Entry pair = (Map.Entry) iter.next();
            Processor wonMessageProcessor = (Processor) pair.getValue();
            Annotation annotation = AopUtils.getTargetClass(wonMessageProcessor).getAnnotation(annotationClazz);
            if (matches(annotation, messageType, direction, null)) {
                slipBuilder.append("bean:").append(pair.getKey().toString()).append("?method=process,");
            }
        }
        String slip = slipBuilder.toString();
        if (!slip.isEmpty()) {
            return slip.substring(0, slip.length() - 1); // cut off the trailing comma
        }
        if (allowNoMatchingProcessor) {
            return null;
        }
        logger.debug("unexpected combination of messageType {} and direction {} encountered "
                        + "- this causes an exception,which triggers a FailureResponse", messageType, direction);
        throw new WonMessageProcessingException(
                        String.format("unexpected combination of messageType %s " + "and direction %s encountered",
                                        messageType, direction));
    }

    private boolean matches(Annotation annotation, URI messageType, URI direction, URI socketType)
                    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (annotation == null || messageType == null || direction == null)
            return false;
        if (!annotationFeatureMatches(annotation, messageType.toString(), "messageType")) {
            return false;
        }
        if (!annotationFeatureMatches(annotation, direction.toString(), "direction")) {
            return false;
        }
        if (socketType != null && !annotationFeatureMatches(annotation, socketType.toString(), "socketType")) {
            return false;
        }
        return true;
    }

    /**
     * An annotation feature matches if it is not specified (default value "ANY") or
     * it is specified and its value is equal to the expected value.
     * 
     * @param annotation
     * @param expected
     * @param featureName
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private boolean annotationFeatureMatches(Annotation annotation, String expected, String featureName)
                    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object actualValue = annotation.annotationType().getDeclaredMethod(featureName).invoke(annotation);
        return "ANY".equals(actualValue) || expected.equals(actualValue);
    }
}
