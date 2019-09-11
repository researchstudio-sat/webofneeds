package won.protocol.model;

import javax.persistence.*;
import java.net.URI;
import java.util.Objects;

/**
 * User: Danijel Date: 28.5.14.
 */
@Entity
@Table(name = "bastate", uniqueConstraints = @UniqueConstraint(columnNames = { "coordinatorURI", "participantURI" }))
public class BAState {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
    /* The URI of the coordinator */
    @Column(name = "coordinatorURI")
    @Convert(converter = URIConverter.class)
    private URI coordinatorURI;
    /* The URI of the participant */
    @Column(name = "participantURI")
    @Convert(converter = URIConverter.class)
    private URI participantURI;
    /* The state of the atom */
    @Column(name = "baStateURI")
    @Convert(converter = URIConverter.class)
    private URI baStateURI;
    @Column(name = "socketTypeURI")
    @Convert(converter = URIConverter.class)
    private URI socketTypeURI;
    @Column(name = "baPhase")
    @Convert(converter = URIConverter.class)
    private URI baPhaseURI;

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BAState))
            return false;
        final BAState baState = (BAState) o;
        if (!Objects.equals(baStateURI, baState.baStateURI))
            return false;
        if (!Objects.equals(coordinatorURI, baState.coordinatorURI))
            return false;
        if (!Objects.equals(socketTypeURI, baState.socketTypeURI))
            return false;
        if (!Objects.equals(id, baState.id))
            return false;
        if (!Objects.equals(participantURI, baState.participantURI))
            return false;
        if (!Objects.equals(baPhaseURI, baState.baPhaseURI))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (coordinatorURI != null ? coordinatorURI.hashCode() : 0);
        result = 31 * result + (participantURI != null ? participantURI.hashCode() : 0);
        result = 31 * result + (baStateURI != null ? baStateURI.hashCode() : 0);
        result = 31 * result + (socketTypeURI != null ? socketTypeURI.hashCode() : 0);
        result = 31 * result + (baPhaseURI != null ? baPhaseURI.hashCode() : 0);
        return result;
    }

    public URI getCoordinatorURI() {
        return coordinatorURI;
    }

    public void setCoordinatorURI(final URI coordinatorURI) {
        this.coordinatorURI = coordinatorURI;
    }

    public URI getParticipantURI() {
        return participantURI;
    }

    public void setParticipantURI(final URI participantURI) {
        this.participantURI = participantURI;
    }

    public URI getBaStateURI() {
        return baStateURI;
    }

    public void setBaStateURI(final URI baStateURI) {
        this.baStateURI = baStateURI;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public URI getSocketTypeURI() {
        return socketTypeURI;
    }

    public void setSocketTypeURI(final URI socketTypeURI) {
        this.socketTypeURI = socketTypeURI;
    }

    public URI getBaPhaseURI() {
        return baPhaseURI;
    }

    public void setBaPhaseURI(final URI baPhaseURI) {
        this.baPhaseURI = baPhaseURI;
    }
}
