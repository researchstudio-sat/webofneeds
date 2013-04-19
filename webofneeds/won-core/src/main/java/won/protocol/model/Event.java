package won.protocol.model;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.persistence.*;
import java.net.URI;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 17.04.13
 * Time: 14:24
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue
    @Column( name = "id" )
    private Long id;

    /* The state of the connection */
    @Column( name = "type")
    private EventType type;

    /* The creation date of the event */
    @Temporal(TemporalType.TIMESTAMP)
    @Column( name = "creationDate", nullable = false)
    private Date creationDate;

    /* The creation date of the need */
    @Column( name = "connectionURI")
    private URI connectionURI;

    @PrePersist
    protected void onCreate() {
        creationDate = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public URI getConnectionURI() {
        return connectionURI;
    }

    public void setConnectionURI(URI connectionURI) {
        this.connectionURI = connectionURI;
    }

    @Override
    public String toString()
    {
        return "Need{" +
                "id=" + id +
                ", type=" + type +
                ", creationDate=" + creationDate +
                ", connectionURI=" + connectionURI +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        if (connectionURI != null ? !connectionURI.equals(event.connectionURI) : event.connectionURI != null)
            return false;
        if (creationDate != null ? !creationDate.equals(event.creationDate) : event.creationDate != null) return false;
        if (type != event.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (connectionURI != null ? connectionURI.hashCode() : 0);
        return result;
    }

    public static void main(String args[]) {
        Configuration config =
                new Configuration();
        config.addAnnotatedClass(Need.class);
        config.configure();
        new SchemaExport(config).create(true, true);
    }
}
