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

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.node.camel.processor.annotation.DefaultSocketMessageProcessor;
import won.node.camel.processor.annotation.SocketMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * User: syim Date: 11.03.2015
 */
public class SocketTypeSlipComputer implements InitializingBean, ApplicationContextAware, Expression {
    private HashMap<String, Object> socketMessageProcessorsMap;
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        socketMessageProcessorsMap = (HashMap) applicationContext.getBeansWithAnnotation(SocketMessageProcessor.class);
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
        URI socketType = (URI) exchange.getIn().getHeader(WonCamelConstants.SOCKET_TYPE_HEADER);
        String slip = "bean:" + computeSocketSlip(messageType, socketType, direction) + "?method=process";
        return type.cast(slip);
    }

    private String computeSocketSlip(URI messageType, URI socketType, URI direction) {
        if (socketType != null) {
            Optional<String> processorName = socketMessageProcessorsMap.entrySet().stream().filter(entry -> {
                Object socket = entry.getValue();
                Annotation annotation = AopUtils.getTargetClass(socket).getAnnotation(SocketMessageProcessor.class);
                return matches(annotation, messageType, direction, socketType);
            }).findFirst().map(Map.Entry::getKey);
            if (processorName.isPresent()) {
                return processorName.get();
            }
        }
        Optional<String> processorName = socketMessageProcessorsMap.entrySet().stream().filter(entry -> {
            Object socket = entry.getValue();
            Annotation annotation = AopUtils.getTargetClass(socket).getAnnotation(DefaultSocketMessageProcessor.class);
            return matches(annotation, messageType, direction, null);
        }).findFirst().map(Map.Entry::getKey);
        if (processorName.isPresent()) {
            return processorName.get();
        }
        throw new WonMessageProcessingException(String.format(
                        "unexpected combination of messageType %s, " + "socketType %s and direction %s encountered",
                        messageType, socketType, direction));
    }

    private boolean matches(Annotation annotation, URI messageType, URI direction, URI socketType) {
        if (annotation == null || messageType == null || direction == null)
            return false;
        try {
            if (annotationFeatureMismatch(annotation, messageType.toString(), "messageType")
                            || annotationFeatureMismatch(annotation, direction.toString(), "direction")) {
                return false;
            }
            if (socketType != null && annotationFeatureMismatch(annotation, socketType.toString(), "socketType")) {
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
