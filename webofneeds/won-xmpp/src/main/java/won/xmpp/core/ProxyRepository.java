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

package won.xmpp.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: Ashkan
 * Date: 12.04.13
 */
public class ProxyRepository {

    Map<String, NeedProxy> proxyMap;

    public ProxyRepository() {
        this.proxyMap = new ConcurrentHashMap<String, NeedProxy>();
    }

    public void addProxy(String jid, NeedProxy np){
        proxyMap.put(jid,np);
    }

    public NeedProxy addProxy(String thisProxyJid, String thisOwnerJid, String otherProxyJid){

        NeedProxy needProxy = new NeedProxy(thisProxyJid, new Owner(thisOwnerJid), otherProxyJid);
        addProxy(thisProxyJid  ,needProxy );
        return needProxy;
    }

    public NeedProxy getNeedProxy(String jid){
        return proxyMap.get(jid);
    }

    public boolean hasProxy(String jid){
        return proxyMap.containsKey(jid);
    }

    public NeedProxy removeProxy(String jid){
        return proxyMap.remove(jid);
    }

}
