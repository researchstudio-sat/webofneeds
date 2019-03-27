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

import java.io.StringWriter;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.message.WonMessage;
import won.protocol.message.processor.exception.WonMessageProcessingException;

/**
 * Last processor for outgoing messages. It expects a WonMessage object in the
 * exchange's in, in the header 'wonMessgage'. The message is serialized as TRIG
 * and put in the in's body.
 */
public class WonMessageFromHeaderToBodySerializingCamelProcessor implements Processor {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(final Exchange exchange) throws Exception {
        logger.debug("processing won message");
        Map headers = exchange.getIn().getHeaders();
        // if the wonMessage header is there, don't change it - that way we can re-route
        // internal messages
        WonMessage wonMessage = (WonMessage) headers.get(WonCamelConstants.MESSAGE_HEADER);
        if (wonMessage == null) {
            throw new WonMessageProcessingException(
                            "No WonMessage found in header '" + WonCamelConstants.MESSAGE_HEADER + "'");
        }
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, wonMessage.getCompleteDataset(), Lang.TRIG);
        exchange.getIn().setBody(writer.toString());
        logger.debug("wrote serialized wonMessage to message body");
    }
}
