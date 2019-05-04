package won.protocol.model;

import java.net.URI;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 28.08.13 Time: 16:03 To
 * change this template use File | Settings | File Templates.
 */
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "socket", indexes = { @Index(name = "IDX_UNIQUE_SOCKET", columnList = "socketURI") })
public class Socket {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
    /* The uri of the socket's atom object */
    @Column(name = "atomURI")
    @Convert(converter = URIConverter.class)
    private URI atomURI;
    /* The uri of the socket's type */
    @Column(name = "typeURI")
    @Convert(converter = URIConverter.class)
    private URI typeURI;
    /* The uri of the socket - must be defined in the atom's content */
    @Column(name = "socketURI")
    @Convert(converter = URIConverter.class)
    private URI socketURI;
    @Column(name = "defaultSocket")
    @Convert(converter = BooleanTFConverter.class)
    private boolean isDefaultSocket = false;

    public Long getId() {
        return id;
    }

    public URI getAtomURI() {
        return atomURI;
    }

    public void setAtomURI(URI atomURI) {
        this.atomURI = atomURI;
    }

    public URI getTypeURI() {
        return typeURI;
    }

    public void setTypeURI(URI typeURI) {
        this.typeURI = typeURI;
    }

    public URI getSocketURI() {
        return socketURI;
    }

    public void setSocketURI(URI socketURI) {
        this.socketURI = socketURI;
    }

    public boolean isDefaultSocket() {
        return isDefaultSocket;
    }

    public void setDefaultSocket(boolean isDefaultSocket) {
        this.isDefaultSocket = isDefaultSocket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Socket socket = (Socket) o;
        if (id != null ? !id.equals(socket.id) : socket.id != null)
            return false;
        if (atomURI != null ? !atomURI.equals(socket.atomURI) : socket.atomURI != null)
            return false;
        if (typeURI != null ? !typeURI.equals(socket.typeURI) : socket.typeURI != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (atomURI != null ? atomURI.hashCode() : 0);
        result = 31 * result + (typeURI != null ? typeURI.hashCode() : 0);
        return result;
    }
}
