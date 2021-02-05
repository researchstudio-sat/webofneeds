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
package won.protocol.message.sender;

import won.protocol.message.WonMessage;
import won.protocol.message.sender.exception.WonMessageSenderException;

import java.net.URI;

/**
 * Interface for sending messages.
 */
public interface WonMessageSender {
    /**
     * Sends the message after signing it and setting its message URI.
     * <p>
     * Equivalent to
     *
     * <pre>
     * WonMessageSencer sender = ... // sender instance
     * WonMessage msg = ... // message (unprepared)
     * WonMessage msg =  sender.prepareMessage(msg);
     *   sender.sendMessage(msg);
     * </pre>
     * </p>
     * Only that you have no access to the prepared WonMessage object.
     *
     * @param message
     * @throws WonMessageSenderException
     */
    void prepareAndSendMessage(WonMessage message) throws WonMessageSenderException;

    /**
     * Send on behalf (ie, signed with a key that is not the sender's).
     *
     * @param message
     * @param webId
     * @throws WonMessageSenderException
     */
    void prepareAndSendMessageOnBehalf(WonMessage message, URI webId) throws WonMessageSenderException;

    /**
     * Signs the message, calculates its messageURI based on content.
     *
     * @param message
     * @return the updated, final message.
     * @throws WonMessageSenderException
     */
    WonMessage prepareMessage(WonMessage message) throws WonMessageSenderException;

    /**
     * Prepare message to send on behalf (ie, signed with a key that is not the
     * sender's).
     *
     * @param message
     * @param webId
     * @return
     * @throws WonMessageSenderException
     */
    WonMessage prepareMessageOnBehalf(WonMessage message, URI webId) throws WonMessageSenderException;

    /**
     * Sends the <b>prepared</b> message. Will fail if the message has not been
     * prepared using <code>prepareMessage</code>.
     *
     * @param message
     * @return the updated, final message.
     * @throws WonMessageSenderException
     */
    void sendMessage(WonMessage message) throws WonMessageSenderException;
}
