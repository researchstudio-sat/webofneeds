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
package won.node.socket.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * User: Danijel Date: 5.6.14.
 */
public class WON_TX {
    // public static final String BASE_URI = "https://w3id.org/won/tx/model#";
    public static final String BASE_URI = "https://w3id.org/won/tx/model#";
    public static final String DEFAULT_PREFIX = "won-tx";
    private static Model m = ModelFactory.createDefaultModel();
    public static final Property COORDINATION_MESSAGE = m.createProperty(BASE_URI + "coordinationMessage");
    public static final Property COORDINATOR_VOTE_REQUEST = m.createProperty(BASE_URI + "coordinatorVoteRequest");
    public static final Property BA_STATE = m.createProperty(BASE_URI + "hasBAState");
    public static final Resource COORDINATOR = m.createResource(BASE_URI + "Coordinator");
    public static final Resource PARTICIPANT = m.createResource(BASE_URI + "Participant");
    public static final Resource COORDINATION_MESSAGE_ABORT = m.createResource(BASE_URI + "Abort");
    public static final Resource COORDINATION_MESSAGE_COMMIT = m.createResource(BASE_URI + "Commit");
    public static final Resource COORDINATION_MESSAGE_ABORT_AND_COMPENSATE = m
                    .createResource(BASE_URI + "AbortAndCompensate");
    // Business Activities
    public static final Resource MESSAGE_CANCEL = m.createResource(BASE_URI + "MessageCancel");
    public static final Resource MESSAGE_CLOSE = m.createResource(BASE_URI + "MessageClose");
    public static final Resource MESSAGE_COMPENSATE = m.createResource(BASE_URI + "MessageCompensate");
    public static final Resource MESSAGE_FAILED = m.createResource(BASE_URI + "MessageFailed");
    public static final Resource MESSAGE_EXITED = m.createResource(BASE_URI + "MessageExited");
    public static final Resource MESSAGE_NOTCOMPLETED = m.createResource(BASE_URI + "MessageNotCompleted");
    public static final Resource MESSAGE_EXIT = m.createResource(BASE_URI + "MessageExit");
    public static final Resource MESSAGE_COMPLETED = m.createResource(BASE_URI + "MessageCompleted");
    public static final Resource MESSAGE_FAIL = m.createResource(BASE_URI + "MessageFail");
    public static final Resource MESSAGE_CANNOTCOMPLETE = m.createResource(BASE_URI + "MessageCanNotComplete");
    public static final Resource MESSAGE_CANCELED = m.createResource(BASE_URI + "MessageCanceled");
    public static final Resource MESSAGE_CLOSED = m.createResource(BASE_URI + "MessageClosed");
    public static final Resource MESSAGE_COMPENSATED = m.createResource(BASE_URI + "MessageCompensated");
    public static final Resource STATE_CLOSING = m.createResource(BASE_URI + "Closing");
    public static final Resource STATE_ACTIVE = m.createResource(BASE_URI + "Active");
    public static final Resource STATE_COMPLETED = m.createResource(BASE_URI + "Completed");
    public static final Resource STATE_CANCELING = m.createResource(BASE_URI + "Canceling");
    public static final Resource STATE_COMPENSATING = m.createResource(BASE_URI + "Compensating");
    public static final Resource STATE_COMPLETING = m.createResource(BASE_URI + "Completing");
    public static final Resource STATE_CANCELING_ACTIVE = m.createResource(BASE_URI + "CancelingActive");
    public static final Resource STATE_CANCELING_COMPLETING = m.createResource(BASE_URI + "CancelingCompleting");
    // from BOTS
    public static final Resource MESSAGE_COMPLETE = m.createResource(BASE_URI + "MessageComplete");
    public static final Property STATE = m.createProperty(BASE_URI + "hasState");
    public static final Resource STATE_ENDED = m.createResource(BASE_URI + "Ended");
    public static final Resource STATE_EXITING = m.createResource(BASE_URI + "Exiting");
    public static final Resource STATE_NOT_COMPLETING = m.createResource(BASE_URI + "NotCompleting");
    public static final Resource STATE_FAILING_COMPENSATING = m.createResource(BASE_URI + "FailingCompensating");
    public static final Resource STATE_FAILING_ACTIVE_CANCELING_COMPLETING = m
                    .createResource(BASE_URI + "FailingActiveCancelingCompleting");
    public static final Resource STATE_FAILING_ACTIVE_CANCELING = m.createResource(BASE_URI + "FailingActiveCanceling");
    public static final Property HAS_TEXT_MESSAGE = m.createProperty(BASE_URI + "textMessage");
    public static final Property PHASE_FIRST = m.createProperty(BASE_URI + "baPhaseFIRST");
    public static final Property PHASE_SECOND = m.createProperty(BASE_URI + "baPhaseSECOND");
    public static final Property PHASE_NONE = m.createProperty(BASE_URI + "baPhaseNONE");
    public static final Property PHASE_CANCELED_FROM_COORDINATOR = m
                    .createProperty(BASE_URI + "baPhaseCANCELED_FROM_COORDINATOR");

    /**
     * returns the URI for this schema
     * 
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
