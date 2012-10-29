import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.authorization.UserAuthorization;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 29.10.12
 * Time: 13:15
 * To change this template use File | Settings | File Templates.
 */
public class WONUserAuthorization implements UserAuthorization, AccountManagement {


    private final Map<Entity, String> userPasswordMap = new HashMap<Entity, String>();

    public WONUserAuthorization() {
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

    @Override
    public boolean verifyCredentials(String username, String passwordCleartext, Object credentials) {
        return verify(EntityImpl.parseUnchecked(username).getBareJID(), passwordCleartext);
    }

    public void removeUser(Entity jid) {
        userPasswordMap.remove(jid.getBareJID());
    }
}
