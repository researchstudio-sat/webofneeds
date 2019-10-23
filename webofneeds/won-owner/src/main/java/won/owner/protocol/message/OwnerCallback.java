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
package won.owner.protocol.message;

import java.net.URI;

import won.protocol.message.WonMessage;
import won.protocol.model.Connection;

/**
 * Interface for owner implementations providing methods for receiving specific
 * messages and sending generic messages.
 */
public interface OwnerCallback {
    public void onConnectFromOtherAtom(Connection con, final WonMessage wonMessage);

    public void onCloseFromOtherAtom(Connection con, final WonMessage wonMessage);

    public void onAtomHintFromMatcher(final WonMessage wonMessage);

    public void onSocketHintFromMatcher(final WonMessage wonMessage);

    public void onMessageFromOtherAtom(Connection con, final WonMessage wonMessage);

    /**
     * Called when a message is received that indicates some error during processing
     * of a message previously sent by the bot.
     * 
     * @param failedMessageUri
     * @param wonMessage
     */
    public void onFailureResponse(URI failedMessageUri, WonMessage wonMessage);

    /**
     * Called when a message is received that indicates successful processing of a
     * message previously sent by the bot.
     * 
     * @param successfulMessageUri
     * @param wonMessage
     */
    public void onSuccessResponse(URI successfulMessageUri, WonMessage wonMessage);
}
