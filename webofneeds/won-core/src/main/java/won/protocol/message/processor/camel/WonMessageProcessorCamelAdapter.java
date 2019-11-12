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
package won.protocol.message.processor.camel;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.exception.WonProtocolException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.util.LoggingUtils;

/**
 * Adapts a WonMessageProcessor to act as a camel processor. The WonMessage
 * object is expected to be found in <code>exchange.getIn()</code> in the
 * 'wonMessage' header. After successful processing, the resulting wonMessage
 * object replaces the original one.
 */
public class WonMessageProcessorCamelAdapter implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private WonMessageProcessor adaptee;
    private List<String> headersHoldingWonMessage;
    private int allowMissing;

    protected WonMessageProcessorCamelAdapter(WonMessageProcessor adaptee) {
        this(adaptee, 0, WonCamelConstants.MESSAGE_HEADER);
    }

    protected WonMessageProcessorCamelAdapter(WonMessageProcessor adaptee, String header) {
        this(adaptee, 0, header);
    }

    protected WonMessageProcessorCamelAdapter(WonMessageProcessor adaptee, String... headers) {
        this(adaptee, 0, headers);
    }

    public WonMessageProcessorCamelAdapter(WonMessageProcessor adaptee, int allowMissing, String... headers) {
        Objects.requireNonNull(adaptee);
        Objects.requireNonNull(headers);
        if (headers.length == 0)
            throw new IllegalArgumentException("at least one header must be specified");
        this.adaptee = adaptee;
        this.allowMissing = allowMissing;
        this.headersHoldingWonMessage = Arrays.asList(headers);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        headersHoldingWonMessage.stream().reduce(0,
                        (missed, header) -> missed + (processHeader(exchange, header, missed < allowMissing) ? 0 : 1),
                        Integer::sum);
    }

    /**
     * Returns true if the header was processed properly, false if the header was
     * not found and headerMayBeMissing was true
     * 
     * @param exchange
     * @param header
     * @return
     * @throws Exception
     */
    private boolean processHeader(Exchange exchange, String header, boolean headerMayBeMissing) {
        Object msg = exchange.getIn().getHeader(header);
        if (msg == null) {
            if (headerMayBeMissing) {
                return false;
            }
            throw new IllegalArgumentException("expected a WonMessage object in the '" + header
                            + " header but header was null");
        }
        if (!(msg instanceof WonMessage)) {
            throw new IllegalArgumentException("expected a WonMessage object in the '" + header
                            + " header but the object is of type " + msg.getClass());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("calling adaptee {} with message {}",
                            new Object[] { adaptee, ((WonMessage) msg).toShortStringForDebug() });
        }
        // call the process method
        WonMessage resultMsg;
        try {
            resultMsg = adaptee.process((WonMessage) msg);
            if (logger.isDebugEnabled()) {
                logger.debug("returning from adaptee {} with message {}",
                                new Object[] { adaptee, ((WonMessage) msg).toShortStringForDebug() });
            }
        } catch (WonProtocolException wpe) {
            // no need to log this, it's an expected exception that will be reported to the
            // client
            throw wpe;
        } catch (Exception e) {
            LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e,
                            "re-throwing exception {} caught calling adaptee {} with message {}",
                            new Object[] { e, adaptee, ((WonMessage) msg).toShortStringForDebug() });
            throw e;
        }
        // set the result of the call as the new message in the exchange's in
        exchange.getIn().setHeader(header, resultMsg);
        return true;
    }
}
