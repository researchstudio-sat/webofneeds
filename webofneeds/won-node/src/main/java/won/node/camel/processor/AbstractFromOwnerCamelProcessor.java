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

/**
 * Base class for processors handling messages coming from a remote node.
 */
public abstract class AbstractFromOwnerCamelProcessor extends AbstractCamelProcessor
    implements FromOwnerCamelProcessor {
  /**
   * Default implementation in case implementors don't need to react to success.
   * Does nothing.
   * 
   * @param exchange
   * @throws Exception
   */
  public void onSuccessResponse(Exchange exchange) throws Exception {
    logger.debug("received success response");
  };

  /**
   * Default implementation in case implementors don't need to react to failure.
   * Does nothing.
   * 
   * @param exchange
   * @throws Exception
   */
  public void onFailureResponse(Exchange exchange) throws Exception {
    logger.debug("processing failure response");
  };

}
