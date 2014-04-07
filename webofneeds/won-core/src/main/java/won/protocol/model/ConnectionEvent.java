package won.protocol.model;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.persistence.*;
import java.net.URI;
import java.util.Date;

/**
 * User: gabriel
 * Date: 17.04.13
 * Time: 14:24
 */
@Entity
@Table(name = "event")
public class ConnectionEvent
{

  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  /* The state of the connection */
  @Column(name = "type")
  private ConnectionEventType type;

  /* The creation date of the event */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "creationDate", nullable = false)
  private Date creationDate;

  @Column(name = "connectionURI")
  private URI connectionURI;

  @Column(name = "originatorURI")
  private URI originatorUri;

  @PrePersist
  protected void onCreate()
  {
    creationDate = new Date();
  }

  public Long getId()
  {
    return id;
  }

  public void setId(Long id)
  {
    this.id = id;
  }

  public ConnectionEventType getType()
  {
    return type;
  }

  public void setType(ConnectionEventType type)
  {
    this.type = type;
  }

  public Date getCreationDate()
  {
    return creationDate;
  }

  public void setCreationDate(Date creationDate)
  {
    this.creationDate = creationDate;
  }

  public URI getConnectionURI()
  {
    return connectionURI;
  }

  public void setConnectionURI(URI connectionURI)
  {
    this.connectionURI = connectionURI;
  }

  public URI getOriginatorUri()
  {
    return originatorUri;
  }

  public void setOriginatorUri(final URI originatorUri)
  {
    this.originatorUri = originatorUri;
  }

  @Override
  public String toString()
  {
    return "Event{" +
        "id=" + id +
        ", type=" + type +
        ", creationDate=" + creationDate +
        ", connectionURI=" + connectionURI +
        ", originatorURI=" + originatorUri +
        '}';
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConnectionEvent connectionEvent = (ConnectionEvent) o;

    if (connectionURI != null ? !connectionURI.equals(connectionEvent.connectionURI) : connectionEvent.connectionURI != null)
      return false;
    if (creationDate != null ? !creationDate.equals(connectionEvent.creationDate) : connectionEvent.creationDate != null)
      return false;
    if (type != connectionEvent.type) return false;
    if (originatorUri != null ? !originatorUri.equals(connectionEvent.getOriginatorUri()) : connectionEvent.originatorUri != null)
      return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = type.hashCode();
    result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
    result = 31 * result + (connectionURI != null ? connectionURI.hashCode() : 0);
    result = 31 * result + (originatorUri != null ? originatorUri.hashCode() : 0);
    return result;
  }

  public static void main(String args[])
  {
    Configuration config =
        new Configuration();
    config.addAnnotatedClass(Need.class);
    config.configure();
    new SchemaExport(config).create(true, true);
  }
}
