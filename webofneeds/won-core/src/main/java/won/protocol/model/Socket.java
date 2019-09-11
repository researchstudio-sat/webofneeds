package won.protocol.model;

import javax.persistence.*;
import java.net.URI;
import java.util.Objects;

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
        if (!Objects.equals(id, socket.id))
            return false;
        if (!Objects.equals(atomURI, socket.atomURI))
            return false;
        if (!Objects.equals(typeURI, socket.typeURI))
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
