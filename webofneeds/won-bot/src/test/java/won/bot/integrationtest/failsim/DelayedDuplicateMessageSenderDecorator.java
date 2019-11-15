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
package won.bot.integrationtest.failsim;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.protocol.message.WonMessage;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.message.sender.exception.WonMessageSenderException;

/**
 * Decorates the EventListenerContext such that the bot sends each message
 * twice.
 */
public class DelayedDuplicateMessageSenderDecorator extends BaseEventListenerContextDecorator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private long delay = 100;

    public DelayedDuplicateMessageSenderDecorator(EventListenerContext delegate) {
        super(delegate);
    }

    public DelayedDuplicateMessageSenderDecorator(EventListenerContext delegate, long delay) {
        super(delegate);
        this.delay = delay;
    }

    @Override
    public WonMessageSender getWonMessageSender() {
        final WonMessageSender delegate = super.getWonMessageSender();
        return new WonMessageSender() {
            @Override
            public void sendMessage(WonMessage message) throws WonMessageSenderException {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                }
                delegate.sendMessage(message);
            }

            @Override
            public WonMessage prepareMessage(WonMessage message) throws WonMessageSenderException {
                return delegate.prepareMessage(message);
            }

            @Override
            public void prepareAndSendMessage(WonMessage message) throws WonMessageSenderException {
                sendMessage(prepareMessage(message));
            }
        };
    }
}
