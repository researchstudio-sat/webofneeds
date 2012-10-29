import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 29.10.12
 * Time: 15:33
 * To change this template use File | Settings | File Templates.
 */
public class WONRoom extends Room {
    public WONRoom(Entity jid, String name, RoomType... types) {
        super(jid, name, types);
    }

    @Override
    public Occupant addOccupant(Entity occupantJid, String name) {
        try {
            if(occupantJid.getBareJID().equals(EntityImpl.parse("need_car@localhost")) ||
                occupantJid.getBareJID().equals(EntityImpl.parse("need_money@localhost")))
                return super.addOccupant(occupantJid, name);
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }
}
