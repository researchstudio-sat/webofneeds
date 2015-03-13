package won.node.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import won.node.annotation.DefaultFacetMessageProcessor;
import won.node.annotation.FacetMessageProcessor;
import won.node.annotation.FixedMessageProcessor;
import won.node.refactoring.FacetCamel;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageProcessor;
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
public class WonMessageSlipComputer implements BeanFactoryPostProcessor,Expression
{
  Logger logger = LoggerFactory.getLogger(this.getClass());
  HashMap<String, Object> fixedMessageProcessorsMap;
  HashMap<String, Object> facetMessageProcessorsMap;


  @Override
  public void postProcessBeanFactory(final ConfigurableListableBeanFactory configurableListableBeanFactory)
    throws BeansException {

    fixedMessageProcessorsMap = (HashMap)configurableListableBeanFactory.getBeansWithAnnotation(FixedMessageProcessor
                                                                                              .class);
    facetMessageProcessorsMap =  (HashMap)configurableListableBeanFactory.getBeansWithAnnotation(FacetMessageProcessor
                                                                                              .class);
/*
    for(Object bean: facetMessageProcessorsMap.values()){
      configurableListableBeanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
    }
    for(Object bean: fixedMessageProcessorsMap.values()){
      configurableListableBeanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
    }
*/
  }

  @Override
  public <T> T evaluate(final Exchange exchange, final Class<T> type) {
    WonMessage message = (WonMessage) exchange.getIn().getHeader("wonMessage");
    assert message != null : "wonMessage header must not be null";
    String slip ="";
    // exchange.getIn().setHeader();
    URI messageType = RdfUtils.toUriOrNull(exchange.getIn().getHeader("messageType"));
    assert messageType != null : "messageType header must not be null";
    URI direction = RdfUtils.toUriOrNull(exchange.getIn().getHeader("direction"));
    assert direction != null : "direction header must not be null";
    URI facetType = RdfUtils.toUriOrNull(exchange.getIn().getHeader("facetType"));
    try {
      slip = computeMessageTypeSlip(slip,messageType,direction);
      slip = computeFacetSlip(slip,messageType, facetType, direction);
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

  private String computeFacetSlip(String slip, URI messageType, URI facetType,URI direction)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Iterator iter = facetMessageProcessorsMap.entrySet().iterator();
    while(iter.hasNext()){
      Map.Entry pair = (Map.Entry)iter.next();
      FacetCamel facet = (FacetCamel) pair.getValue();
      if (facetType != null) {
        Annotation annotation = facet.getClass().getAnnotation(FacetMessageProcessor.class);
        if(matches(annotation, messageType, direction, facetType)){
          slip += ","+pair.getKey().toString();
          break;
        }
      } else {
        Annotation annotation = facet.getClass().getAnnotation(DefaultFacetMessageProcessor.class);
        if(matches(annotation, messageType, direction, facetType)){
          slip += ","+pair.getKey().toString();
          break;
        }
      }
    }
    return slip;
  }

  private String computeMessageTypeSlip(String slip, URI messageType,URI direction)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Iterator iter = fixedMessageProcessorsMap.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry pair = (Map.Entry)iter.next();
      WonMessageProcessor wonMessageProcessor = (WonMessageProcessor)pair.getValue();
      Annotation annotation = wonMessageProcessor.getClass().getAnnotation(FixedMessageProcessor.class);

      if(matches(annotation, messageType, direction, null)){
        slip = pair.getKey().toString();
        break;
      }
    }
    return slip;
  }

  private String addToSlip(String slip, Object key){
    if(slip.length()==0){
      slip += key;
    }else{
      slip += ","+key;
    }
    return slip;
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
