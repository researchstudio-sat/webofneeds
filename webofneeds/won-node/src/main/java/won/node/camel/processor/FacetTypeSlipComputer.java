/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.node.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.node.messaging.processors.DefaultFacetMessageProcessor;
import won.node.messaging.processors.FacetMessageProcessor;
import won.protocol.message.WonMessage;
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
public class FacetTypeSlipComputer implements InitializingBean, ApplicationContextAware, Expression
{
  Logger logger = LoggerFactory.getLogger(this.getClass());
  HashMap<String, Object> facetMessageProcessorsMap;
  private ApplicationContext applicationContext;

  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }


  @Override
  public void afterPropertiesSet() throws Exception {
    facetMessageProcessorsMap =  (HashMap)applicationContext.getBeansWithAnnotation(FacetMessageProcessor
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
    URI facetType = RdfUtils.toUriOrNull(exchange.getIn().getHeader("facetType"));
    try {
      slip = computeFacetSlip(messageType, facetType, direction);
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

  private String computeFacetSlip(URI messageType, URI facetType, URI direction)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Iterator iter = facetMessageProcessorsMap.entrySet().iterator();
    while(iter.hasNext()){
      Map.Entry pair = (Map.Entry)iter.next();
      Object facet =  pair.getValue();
      if (facetType != null) {
        Annotation annotation = facet.getClass().getAnnotation(FacetMessageProcessor.class);
        if(matches(annotation, messageType, direction, facetType)){
          return "bean:"+pair.getKey().toString();
        }
      } else {
        Annotation annotation = facet.getClass().getAnnotation(DefaultFacetMessageProcessor.class);
        if(matches(annotation, messageType, direction, facetType)){
          return "bean:"+pair.getKey().toString();
        }
      }
    }
    throw new WonMessageProcessingException(String.format("unexpected combination of messageType, " +
      "facetType and direction encountered:  %s, %s, %s", messageType, facetType, direction));
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
