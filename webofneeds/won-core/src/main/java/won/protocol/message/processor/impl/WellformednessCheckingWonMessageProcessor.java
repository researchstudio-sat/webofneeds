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

package won.protocol.message.processor.impl;

import com.hp.hpl.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageValidator;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageNotWellFormedException;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.util.RdfUtils;

/**
 * Checks WonMessages for integrity. The following steps are performed:
 * <pre>
 *   <ul>
 *     <li>No default graph may be present</li>
 *     <li>each named graph is either an EnvelopeGraph or referenced in an EnvelopeGraph</li>
 *     <li>The outermost EnvelopeGraph is a subgraph of the graph identified by the message URI</li>
 *   </ul>
 * </pre>
 *
 */
public class WellformednessCheckingWonMessageProcessor implements WonMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());


  @Override
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
    Dataset dataset = message.getCompleteDataset();
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder errorMessage = new StringBuilder("Message is not valid, failed at check ");
    boolean valid = validator.validate(dataset, errorMessage);
    if (!valid) {
      logger.info(errorMessage.toString() + "\n" + RdfUtils.writeDatasetToString(dataset, Lang.TRIG));
      throw new WonMessageNotWellFormedException(errorMessage.toString());
    }
    return message;
  }



}
