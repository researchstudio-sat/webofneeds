package won.protocol.owner;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 31.10.13
 */
public interface OwnerProtocolNeedReadService {//extends OwnerProtocolNeedServiceClientSide{

    public Collection<URI> listNeedURIs();

    public Collection<URI> listNeedURIs(int page);

    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException;


    public Collection<URI> listConnectionURIs();

    public Collection<URI> listConnectionURIs(int page);

    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException;

    public Need readNeed(URI needURI) throws NoSuchNeedException;

    public Model readNeedContent(URI needURI) throws NoSuchNeedException;

    public Connection readConnection(URI connectionURI) throws NoSuchConnectionException;

    public List<ConnectionEvent> readEvents(final URI connectionURI) throws NoSuchConnectionException;

    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException;
}
