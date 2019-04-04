/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.node.camel.processor.general;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

/**
 * User: syim Date: 11.03.2015
 */
public class FacetTypeSlipComputer implements InitializingBean, ApplicationContextAware, Expression {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private HashMap<String, Object> facetMessageProcessorsMap;
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        facetMessageProcessorsMap = (HashMap) applicationContext.getBeansWithAnnotation(FacetMessageProcessor.class);
    }

    @Override
    public <T> T evaluate(final Exchange exchange, final Class<T> type) {
        WonMessage message = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        assert message != null : "wonMessage header must not be null";
        // exchange.getIn().setHeader();
        URI messageType = (URI) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_TYPE_HEADER);
        assert messageType != null : "messageType header must not be null";
        URI direction = (URI) exchange.getIn().getHeader(WonCamelConstants.DIRECTION_HEADER);
        assert direction != null : "direction header must not be null";
        URI facetType = (URI) exchange.getIn().getHeader(WonCamelConstants.FACET_TYPE_HEADER);
        String slip = "bean:" + computeFacetSlip(messageType, facetType, direction) + "?method=process";
        return type.cast(slip);
    }

    private String computeFacetSlip(URI messageType, URI facetType, URI direction) {
        if (facetType != null) {
            Optional<String> processorName = facetMessageProcessorsMap.entrySet().stream().filter(entry -> {
                Object facet = entry.getValue();
                Annotation annotation = AopUtils.getTargetClass(facet).getAnnotation(FacetMessageProcessor.class);
                return matches(annotation, messageType, direction, facetType);
            }).findFirst().map(Map.Entry::getKey);
            if (processorName.isPresent()) {
                return processorName.get();
            }
        }
        Optional<String> processorName = facetMessageProcessorsMap.entrySet().stream().filter(entry -> {
            Object facet = entry.getValue();
            Annotation annotation = AopUtils.getTargetClass(facet).getAnnotation(DefaultFacetMessageProcessor.class);
            return matches(annotation, messageType, direction, null);
        }).findFirst().map(Map.Entry::getKey);
        if (processorName.isPresent()) {
            return processorName.get();
        }
        throw new WonMessageProcessingException(String.format(
                        "unexpected combination of messageType %s, " + "facetType %s and direction %s encountered",
                        messageType, facetType, direction));
    }

    private boolean matches(Annotation annotation, URI messageType, URI direction, URI facetType) {
        if (annotation == null || messageType == null || direction == null)
            return false;
        try {
            if (annotationFeatureMismatch(annotation, messageType.toString(), "messageType")
                            || annotationFeatureMismatch(annotation, direction.toString(), "direction")) {
                return false;
            }
            if (facetType != null && annotationFeatureMismatch(annotation, facetType.toString(), "facetType")) {
                return false;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private boolean annotationFeatureMismatch(Annotation annotation, String expected, String featureName)
                    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return !expected.equals(annotation.annotationType().getDeclaredMethod(featureName).invoke(annotation));
    }
}
