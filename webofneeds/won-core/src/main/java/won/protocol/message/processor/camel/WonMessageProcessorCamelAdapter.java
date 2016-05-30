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

package won.protocol.message.processor.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.WonMessageProcessor;


/**
 * Adapts a WonMessageProcessor to act as a camel processor.
 * The WonMessage object is expected to be found in <code>exchange.getIn()</code>
 * in the 'wonMessage' header. After successful processing,
 * the resulting wonMessage object replaces the original one.
 */
public class WonMessageProcessorCamelAdapter implements Processor {

  public static String WON_MESSAGE_HEADER = "wonMessage";

  private WonMessageProcessor adaptee;

  protected WonMessageProcessorCamelAdapter(WonMessageProcessor adaptee) {
    this.adaptee = adaptee;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Object msg = exchange.getIn().getHeader(WON_MESSAGE_HEADER);
    if (msg == null) {
      throw new IllegalArgumentException("expected a WonMessage object in the '"+ WON_MESSAGE_HEADER + " header but header was null");
    }
    if (! (msg instanceof WonMessage) ) {
      throw new IllegalArgumentException("expected a WonMessage object in the '"+ WON_MESSAGE_HEADER + " header but the object is of type " + msg.getClass());
    }
    //call the process method
    WonMessage resultMsg = adaptee.process((WonMessage) msg);
    //set the result of the call as the new message in the exchange's in
    exchange.getIn().setHeader(WON_MESSAGE_HEADER, resultMsg);
  }
}

