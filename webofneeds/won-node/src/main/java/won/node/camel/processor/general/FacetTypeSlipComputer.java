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

package won.node.camel.processor.general;

import net.bytebuddy.pool.TypePool;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.node.camel.processor.annotation.DefaultFacetMessageProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.vocabulary.WONMSG;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

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
    WonMessage message = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    assert message != null : "wonMessage header must not be null";
    String slip ="";
    // exchange.getIn().setHeader();
    URI messageType = (URI) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_TYPE_HEADER);
    assert messageType != null : "messageType header must not be null";
    URI direction = (URI) exchange.getIn().getHeader(WonCamelConstants.DIRECTION_HEADER);
    assert direction != null : "direction header must not be null";
    URI facetType = (URI) exchange.getIn().getHeader(WonCamelConstants.FACET_TYPE_HEADER);
    //for ordinary messages, the process method is called
    //for responses, the on[Failure|Success]Response is called.
    String method = "process";
    if (WonMessageDirection.FROM_EXTERNAL.isIdentifiedBy(direction)){
      //check if we're handling a response. If so, do special routing
      //the response comes from the remote node, but the handler we need is the
      //one that sent the original message, so we have to switch direction
      //and we have to set the type to the type of the original message that
      //we are now handling the response to
      if (WonMessageType.SUCCESS_RESPONSE.isIdentifiedBy(messageType)){
        method = "onSuccessResponse";
        direction = URI.create(WonMessageDirection.FROM_OWNER.getResource().toString());
        WonMessageType origType = message.getIsResponseToMessageType();
        if (origType == null) {
          throw new MissingMessagePropertyException(URI.create(WONMSG.IS_RESPONSE_TO_MESSAGE_TYPE.getURI().toString()));
        }
        messageType = origType.getURI();
      } else if (WonMessageType.FAILURE_RESPONSE.isIdentifiedBy(messageType)){

        WonMessageType isResponseToType = message.getIsResponseToMessageType();
        if (WonMessageType.FAILURE_RESPONSE == isResponseToType
          || WonMessageType.SUCCESS_RESPONSE == isResponseToType) {
          //exception from the exception: if we're handling a FailureResponse
          // to a response - in that case, don't compute a slip value - no bean
          // will specially process this.
          return null;
        }
        method = "onFailureResponse";
        direction = URI.create(WonMessageDirection.FROM_OWNER.getResource().toString());
        WonMessageType origType = message.getIsResponseToMessageType();
        if (origType == null) {
          throw new MissingMessagePropertyException(
            URI.create(WONMSG.IS_RESPONSE_TO_MESSAGE_TYPE.getURI().toString()));
        }
        messageType = origType.getURI();

      }
    }

    slip = "bean:"+computeFacetSlip(messageType, facetType, direction) + "?method=" + method;
    return type.cast(slip);
  }

  private String computeFacetSlip(URI messageType, URI facetType, URI direction) {

    if(facetType != null) {
      Optional<String> processorName = facetMessageProcessorsMap.entrySet().stream()
              .filter(entry -> {
                Object facet = entry.getValue();
                Annotation annotation = AopUtils.getTargetClass(facet).getAnnotation(FacetMessageProcessor.class);
                return matches(annotation, messageType, direction, facetType);
              }).findFirst().map(entry -> entry.getKey());

      if (processorName.isPresent()) {
        return processorName.get();
      }
    }

    Optional<String> processorName = facetMessageProcessorsMap.entrySet().stream()
            .filter(entry -> {
              Object facet = entry.getValue();
              Annotation annotation = AopUtils.getTargetClass(facet).getAnnotation(DefaultFacetMessageProcessor.class);
              return matches(annotation, messageType, direction, null);
            }).findFirst().map(entry -> entry.getKey());

    if (processorName.isPresent()) {
      return processorName.get();
    }

    throw new WonMessageProcessingException(String.format("unexpected combination of messageType %s, " +
      "facetType %s and direction %s encountered", messageType, facetType, direction));
  }

  private boolean matches(Annotation annotation, URI messageType, URI direction, URI facetType) {
    if (annotation == null || messageType==null||direction==null) return false;
    try {
      if (messageType != null) {
        if (!annotationFeatureMatches(annotation, messageType.toString(), "messageType")) {
          return false;
        }
      }
      if (direction != null) {
        if (!annotationFeatureMatches(annotation, direction.toString(), "direction")) {
          return false;
        }
      }
      if (facetType != null) {
        if (!annotationFeatureMatches(annotation, facetType.toString(), "facetType")) {
          return false;
        }
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return true;
  }

  private boolean annotationFeatureMatches(Annotation annotation, String expected, String featureName)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return expected.equals(annotation.annotationType().getDeclaredMethod(featureName).invoke(annotation));
  }

}
