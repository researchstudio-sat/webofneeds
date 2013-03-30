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

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.authentication.UserAuthentication;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 29.10.12
 * Time: 13:15
 * To change this template use File | Settings | File Templates.
 */
public class WONUserAuthentication implements UserAuthentication, AccountManagement {


    private final Map<Entity, String> userPasswordMap = new HashMap<Entity, String>();

    public WONUserAuthentication() {
        ;
    }

    private boolean verify(Entity username, String passwordCleartext) {
        return passwordCleartext.equals(userPasswordMap.get(username));
    }
    @Override
    public void addUser(Entity username, String password) throws AccountCreationException {
        userPasswordMap.put(username, password);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void changePassword(Entity username, String password) throws AccountCreationException {
        if (!userPasswordMap.containsKey(username)) {
            throw new AccountCreationException("could not change password for unknown user " + username);
        }
        userPasswordMap.put(username, password);
    }

    @Override
    public boolean verifyAccountExists(Entity jid) {
        return userPasswordMap.get(jid.getBareJID()) != null;
    }

    @Override
    public boolean verifyCredentials(Entity jid, String passwordCleartext, Object credentials) {
        return verify(jid.getBareJID(), passwordCleartext);
    }

    public boolean verifyCredentials(String username, String passwordCleartext, Object credentials) {
        return verify(EntityImpl.parseUnchecked(username).getBareJID(), passwordCleartext);
    }

    public void removeUser(Entity jid) {
        userPasswordMap.remove(jid.getBareJID());
    }
}
