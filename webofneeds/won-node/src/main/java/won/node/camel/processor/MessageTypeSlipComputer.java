package won.node.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.node.messaging.processors.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.util.RdfUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: syim
 * Date: 11.03.2015
 */
public class MessageTypeSlipComputer implements InitializingBean, ApplicationContextAware, Expression
{
  Logger logger = LoggerFactory.getLogger(this.getClass());
  HashMap<String, Object> fixedMessageProcessorsMap;
  private ApplicationContext applicationContext;

  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }


  @Override
  public void afterPropertiesSet() throws Exception {
    fixedMessageProcessorsMap = (HashMap)applicationContext.getBeansWithAnnotation(FixedMessageProcessor
            .class);

  }


  @Override
  public <T> T evaluate(final Exchange exchange, final Class<T> type) {
    WonMessage message = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.WON_MESSAGE_HEADER);
    assert message != null : "wonMessage header must not be null";
    String slip ="";
    // exchange.getIn().setHeader();
    URI messageType = RdfUtils.toUriOrNull(exchange.getIn().getHeader("messageType"));
    assert messageType != null : "messageType header must not be null";
    URI direction = RdfUtils.toUriOrNull(exchange.getIn().getHeader("direction"));
    assert direction != null : "direction header must not be null";
    String method = "process";
    if (WonMessageType.SUCCESS_RESPONSE.getResource().getURI().toString().equals(messageType)){
      method = "onSuccessResponse";
      messageType = URI.create(message.getIsResponseToMessageType().getResource().toString());
    } else if (WonMessageType.FAILURE_RESPONSE.getResource().getURI().toString().equals(messageType)){
      method ="onFailureResponse";
      messageType = URI.create(message.getIsResponseToMessageType().getResource().toString());
    }
    try {
      slip = "bean:"+computeMessageTypeSlip(messageType,direction) + "?method=" + method;
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    exchange.getIn().setHeader("wonSlip",slip);
    return type.cast(slip);
  }


  private String computeMessageTypeSlip(URI messageType, URI direction)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Iterator iter = fixedMessageProcessorsMap.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry pair = (Map.Entry)iter.next();
      Processor wonMessageProcessor = (Processor)pair.getValue();
      Annotation annotation = wonMessageProcessor.getClass().getAnnotation(FixedMessageProcessor.class);

      if(matches(annotation, messageType, direction, null)){
        return pair.getKey().toString();
      }
    }
    throw new WonMessageProcessingException(String.format("unexpected combination of messageType " +
      "and direction encountered:  %s, %s", messageType, direction));
  }



  private boolean matches(Annotation annotation, URI messageType, URI direction, URI facetType)
    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    if (annotation == null || messageType==null||direction==null) return false;
    if (messageType != null){
      if (!annotationFeatureMatches(annotation, messageType.toString(), "messageType")){
        return false;
      }
    }
    if (direction != null){
      if (!annotationFeatureMatches(annotation, direction.toString(), "direction")){
        return false;
      }
    }
    if (facetType != null){
      if (!annotationFeatureMatches(annotation, facetType.toString(), "facetType")){
        return false;
      }
    }
    return true;
  }

  private boolean annotationFeatureMatches(Annotation annotation, String expected, String featureName)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return expected.equals(annotation.annotationType().getDeclaredMethod(featureName).invoke(annotation));
  }

}
