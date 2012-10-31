/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.server.xmpp;

import won.server.model.Need;
import won.server.ws.NeedTransaction;

/**
 * Created with IntelliJ IDEA.
 * User: fsalcher
 * Date: 10.10.12
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
public interface XMPPServerService {
    public void registerNewNeed(Need need);
    public void deleteNeed(Need need);

    /**
     * very experimental - final implementation depends on XMPP server API
     *
     * All XMPP users for the group chat can (hopefully be extracted from the NeedTransaction.
     * A handle to the XMPP session should be added to the transaction object.
     */
    public void startGroupChatSession(NeedTransaction transaction);

    public void endGroupChatSession(NeedTransaction transaction);

    public void setupPrivateChannels(NeedTransaction transaction);

    public void endPrivateChannels(NeedTransaction transaction);

}
