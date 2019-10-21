package won.protocol.repository;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import won.protocol.model.Socket;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 10.09.13 Time: 17:14 To
 * change this template use File | Settings | File Templates.
 */
public interface SocketRepository extends WonRepository<Socket> {
    List<Socket> findByAtomURI(URI atomURI);

    List<Socket> findByAtomURIAndTypeURI(URI atomURI, URI typeURI);

    List<Socket> findByAtomURIAndSocketURI(URI atomURI, URI socketURI);

    Optional<Socket> findOneByAtomURIAndTypeURI(URI atomURI, URI typeURI);

    Optional<Socket> findOneByAtomURIAndSocketURI(URI atomURI, URI socketURI);

    Optional<Socket> findOneBySocketURI(URI socketURI);
}
