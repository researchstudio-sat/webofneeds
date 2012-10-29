import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.RoomStorageProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 29.10.12
 * Time: 13:29
 * To change this template use File | Settings | File Templates.
 */
public class WONRoomStorageProvider implements RoomStorageProvider {
    private Map<Entity, Room> rooms = new ConcurrentHashMap<Entity, Room>();

    public void initialize() {
        // do nothing
    }

    public Room createRoom(Entity jid, String name, RoomType... roomTypes) {
        if(name.equals("need")) {
            Room room = new Room(jid, name, roomTypes);
            rooms.put(jid, room);
            return room;
        }
        return null;
    }

    public Collection<Room> getAllRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    public Room findRoom(Entity jid) {
        return rooms.get(jid);
    }

    public boolean roomExists(Entity jid) {
        return rooms.containsKey(jid);
    }

    public void deleteRoom(Entity jid) {
        rooms.remove(jid);

    }
}
