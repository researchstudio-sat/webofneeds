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

/**
 * User: Ashkan
 * Date: 12.04.13
 */
public class NeedProxy {

    String jid;
    String otherProxyJid;
    String status;
    Owner owner;


    public NeedProxy(String jid, String status, Owner owner, String otherProxyJid) {
        this.jid = jid;
        this.status = status;
        this.owner = owner;
        this.otherProxyJid = otherProxyJid;
    }

    public NeedProxy(String jid, Owner owner, String otherNeedJid) {
        this(jid,null,owner,otherNeedJid);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }


    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getOtherProxyJid() {
        return otherProxyJid;
    }

    public void setOtherProxyJid(String otherProxyJid) {
        this.otherProxyJid = otherProxyJid;
    }
}
