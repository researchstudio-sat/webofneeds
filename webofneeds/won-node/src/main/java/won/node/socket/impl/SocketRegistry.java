package won.node.socket.impl;

import java.net.URI;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.DataAccessUtils;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 16.09.13 Time: 18:43 To
 * change this template use File | Settings | File Templates.
 */
public class SocketRegistry {
    @Autowired
    private ConnectionRepository connectionRepository;
    private HashMap<SocketType, SocketLogic> map;

    public SocketLogic get(Connection con) {
        return get(SocketType.getSocketType(con.getTypeURI()));
    }

    public SocketLogic get(URI connectionURI) throws NoSuchConnectionException {
        return get(SocketType.getSocketType(
                        DataAccessUtils.loadConnection(connectionRepository, connectionURI).getTypeURI()));
    }

    public SocketLogic get(SocketType ft) {
        return map.get(ft);
    }

    public void register(SocketType ft, SocketLogic fi) {
        map.put(ft, fi);
    }

    public void setMap(HashMap<SocketType, SocketLogic> map) {
        this.map = map;
    }
}
