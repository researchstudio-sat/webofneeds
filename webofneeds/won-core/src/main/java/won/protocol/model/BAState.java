package won.protocol.model;

import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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

    /* The state of the need */
    @Column(name = "baStateURI")
    @Convert(converter = URIConverter.class)
    private URI baStateURI;

    @Column(name = "facetTypeURI")
    @Convert(converter = URIConverter.class)
    private URI facetTypeURI;

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

        if (baStateURI != null ? !baStateURI.equals(baState.baStateURI) : baState.baStateURI != null)
            return false;
        if (coordinatorURI != null ? !coordinatorURI.equals(baState.coordinatorURI) : baState.coordinatorURI != null)
            return false;
        if (facetTypeURI != null ? !facetTypeURI.equals(baState.facetTypeURI) : baState.facetTypeURI != null)
            return false;
        if (id != null ? !id.equals(baState.id) : baState.id != null)
            return false;
        if (participantURI != null ? !participantURI.equals(baState.participantURI) : baState.participantURI != null)
            return false;
        if (baPhaseURI != null ? !baPhaseURI.equals(baState.baPhaseURI) : baState.baPhaseURI != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (coordinatorURI != null ? coordinatorURI.hashCode() : 0);
        result = 31 * result + (participantURI != null ? participantURI.hashCode() : 0);
        result = 31 * result + (baStateURI != null ? baStateURI.hashCode() : 0);
        result = 31 * result + (facetTypeURI != null ? facetTypeURI.hashCode() : 0);
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

    public URI getFacetTypeURI() {
        return facetTypeURI;
    }

    public void setFacetTypeURI(final URI facetTypeURI) {
        this.facetTypeURI = facetTypeURI;
    }

    public URI getBaPhaseURI() {
        return baPhaseURI;
    }

    public void setBaPhaseURI(final URI baPhaseURI) {
        this.baPhaseURI = baPhaseURI;
    }

}
