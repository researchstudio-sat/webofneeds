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

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.vocabulary.WONMSG;

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
        WonMessage message = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        assert message != null : "wonMessage header must not be null";
        String slip = "";
        // exchange.getIn().setHeader();
        URI messageType = (URI) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_TYPE_HEADER);
        assert messageType != null : "messageType header must not be null";
        URI direction = (URI) exchange.getIn().getHeader(WonCamelConstants.DIRECTION_HEADER);
        assert direction != null : "direction header must not be null";
        try {
            String bean = computeMessageTypeSlip(messageType, direction);
            if (bean == null) {
                return null;
            }
            slip = "bean:" + bean + "?method=process";
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
        while (iter.hasNext()) {
            Map.Entry pair = (Map.Entry) iter.next();
            Processor wonMessageProcessor = (Processor) pair.getValue();
            Annotation annotation = AopUtils.getTargetClass(wonMessageProcessor).getAnnotation(annotationClazz);
            if (matches(annotation, messageType, direction, null)) {
                return pair.getKey().toString();
            }
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

    private boolean matches(Annotation annotation, URI messageType, URI direction, URI facetType)
                    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (annotation == null || messageType == null || direction == null)
            return false;
        if (annotationFeatureMismatch(annotation, messageType.toString(), "messageType")
                        || annotationFeatureMismatch(annotation, direction.toString(), "direction")) {
            return false;
        }
        if (facetType != null && annotationFeatureMismatch(annotation, facetType.toString(), "facetType")) {
            return false;
        }
        return true;
    }

    private boolean annotationFeatureMismatch(Annotation annotation, String expected, String featureName)
                    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return !expected.equals(annotation.annotationType().getDeclaredMethod(featureName).invoke(annotation));
    }
}
