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
    @Column(name = "socketURI", nullable = false)
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((socketURI == null) ? 0 : socketURI.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Socket other = (Socket) obj;
        if (socketURI == null) {
            if (other.socketURI != null)
                return false;
        } else if (!socketURI.equals(other.socketURI))
            return false;
        return true;
    }
}
