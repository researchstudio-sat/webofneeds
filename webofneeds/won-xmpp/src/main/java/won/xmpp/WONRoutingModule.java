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

package won.xmpp;


import org.apache.vysper.xmpp.modules.DefaultModule;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.ServerRuntimeContextService;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import won.protocol.model.Need;
import won.xmpp.core.NeedProxy;
import won.xmpp.core.ProxyRepository;

import java.util.List;

/**
 * User: Ashkan
 * Date: 19.04.13
 */
public class WONRoutingModule extends DefaultModule {

    private ProxyRepository proxyRepository;

    public WONRoutingModule() {
        this.proxyRepository = new ProxyRepository();
    }

    public void addProxy(String thisProxyJid, String thisProxyOwner, String otherProxyJid){

        proxyRepository.addProxy(thisProxyJid,thisProxyOwner, otherProxyJid);
    }

    public NeedProxy getProxy(String jid){
        return proxyRepository.getNeedProxy(jid);
    }

    public boolean hasProxy(String jid){

        return proxyRepository.hasProxy(jid);
    }


    @Override
    public String getName() {
        return "WONRoutingModule";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getVersion() {
        return "0.1";  //To change body of implemented methods use File | Settings | File Templates.
    }


}
