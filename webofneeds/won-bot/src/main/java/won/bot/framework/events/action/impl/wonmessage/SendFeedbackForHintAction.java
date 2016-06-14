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

package won.bot.framework.events.action.impl.wonmessage;

import com.hp.hpl.jena.query.Dataset;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.wonmessage.HintFromMatcherEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Random;

/**
 * User: fkleedorfer
 * Date: 30.01.14
 */
public class SendFeedbackForHintAction extends BaseEventBotAction
{
  //random number generator needed for random feedback value
  Random random = new Random(System.currentTimeMillis());

  public SendFeedbackForHintAction(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  public void doRun(final Event event) throws Exception {
    if (event instanceof HintFromMatcherEvent) {
      //TODO: the hint with a match object is not really suitable here. Would be better to
      // use connection object instead
      HintFromMatcherEvent hintEvent = (HintFromMatcherEvent) event;
      hintEvent.getWonMessage().getReceiverURI();
      boolean feedbackValue = random.nextBoolean();
      WonMessage message = createFeedbackMessage(hintEvent.getWonMessage().getReceiverURI(), feedbackValue);
      logger.debug("sending {} feedback for hint {} in message {}",new Object[]{
                   (feedbackValue ? "positive":"negative"), event, message.getMessageURI()});
      getEventListenerContext().getWonMessageSender().sendWonMessage(message);
    }
  }

  private WonMessage createFeedbackMessage(URI connectionURI,boolean booleanFeedbackValue) throws
    WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    Dataset connectionRDF =
      getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
    URI localNeed = WonRdfUtils.NeedUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
    URI wonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);

    return WonMessageBuilder
      .setMessagePropertiesForHintFeedback(
        wonNodeInformationService.generateEventURI(
          wonNode),
        connectionURI,
        localNeed,
        wonNode, booleanFeedbackValue
      )
      .build();
  }

}
