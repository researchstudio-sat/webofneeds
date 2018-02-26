/*
 * Copyright 2017  Research Studios Austria Forschungsges.m.b.H.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package won.bot.framework.eventbot.action.impl.analyzation;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementCanceledEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementErrorEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.highlevel.HighlevelProtocols;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.utils.goals.GoalInstantiationProducer;
import won.utils.goals.GoalInstantiationResult;

import java.net.URI;
import java.util.Collection;

/**
 * Checks if the received hint is for a factoryURI
 */
public class AnalyzeAction extends BaseEventBotAction {
    public AnalyzeAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        //TODO: Implement the analyzation accordingly, currently we will push the events by sending the event given as a textmessage (for debug purposes)
        EventListenerContext ctx = getEventListenerContext();
        BotContextWrapper botContextWrapper = ctx.getBotContextWrapper();
        EventBus bus = ctx.getEventBus();
        LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

        if(event instanceof WonMessageReceivedOnConnectionEvent){
            WonMessageReceivedOnConnectionEvent receivedOnConnectionEvent = (WonMessageReceivedOnConnectionEvent) event;
            Connection con = receivedOnConnectionEvent.getCon();

            URI acceptedEventUri = WonRdfUtils.MessageUtils.getAcceptedEvent(receivedOnConnectionEvent.getWonMessage());
            if(WonRdfUtils.MessageUtils.isAccepts(receivedOnConnectionEvent.getWonMessage())){
                logger.debug("ACCEPT MESSAGE FOUND "+acceptedEventUri);
                //IF ACCEPTS MESSAGE -> ACCEPT AGREEMENT
                Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);
                Model agreementPayload = HighlevelProtocols.getAgreement(fullConversationDataset, receivedOnConnectionEvent.getWonMessage().getCorrespondingRemoteMessageURI().toString()); //TODO: RETRIEVE AGREEMENT, for whatever reason there is no agreement at this point only a proposal idk why
                bus.publish(new AgreementAcceptedEvent(con, agreementPayload));
            }else {
                //TODO: DETERMINE WHAT MESSAGE THIS ACTUALLY IS
                //IF CANCELS MESSAGE -> CANCEL AGREEMENT
                //IF PROPOSES MESSAGE -> CHECK PROPOSAL VALIDITY AND STUFF
                //IF ANY MESSAGE -> SEE IF GOALINSTANTIATION IS FULFILLED


                Dataset needDataset = linkedDataSource.getDataForResource(receivedOnConnectionEvent.getNeedURI());
                Dataset remoteNeedDataset = linkedDataSource.getDataForResource(receivedOnConnectionEvent.getRemoteNeedURI());
                Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);

                GoalInstantiationProducer goalInstantiationProducer = new GoalInstantiationProducer(needDataset, remoteNeedDataset, conversationDataset, "http://example.org/", "http://example.org/blended/");
                Collection<GoalInstantiationResult> results = goalInstantiationProducer.createGoalInstantiationResultsForNeed1();


                /*Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);
                Dataset agreements = HighlevelProtocols.getAgreements(fullConversationDataset);
                RDFDataMgr.write(System.out, agreements, Lang.TRIG);
                Dataset proposals = HighlevelProtocols.getProposals(fullConversationDataset);*/

                for (GoalInstantiationResult result : results) {
                    if (result.getShaclReportWrapper().isConform()) {
                        bus.publish(new PreconditionMetEvent(con, result));
                    } else {
                        bus.publish(new PreconditionUnmetEvent(con, result));
                    }
                }
                //TODO: THIS IS SOLELY FOR DEBUG PURPOSES NOW
                if (event instanceof MessageFromOtherNeedEvent) {
                    logger.info("Analyzing MessageFromOtherNeedEvent");
                    MessageFromOtherNeedEvent messageFromOtherNeedEvent = (MessageFromOtherNeedEvent) event;

                    String textMessage = WonRdfUtils.MessageUtils.getTextMessage(messageFromOtherNeedEvent.getWonMessage());

                    if ("AgreementCanceledEvent".equals(textMessage)) {
                        bus.publish(new AgreementCanceledEvent(con, null));
                    } else if ("AgreementErrorEvent".equals(textMessage)) {
                        bus.publish(new AgreementErrorEvent(con, null));
                    }
                } else {
                    logger.error("AnalyzeAction can only handle MessageFromOtherNeedEvent and OpenFromOtherNeedEvent");
                    return;
                }
            }
        } else {
            logger.error("AnalyzeAction can only handle WonMessageReceivedOnConnectionEvent");
            return;
        }
    }
}
