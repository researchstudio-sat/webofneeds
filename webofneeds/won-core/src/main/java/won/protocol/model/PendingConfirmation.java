package won.protocol.model;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(indexes = @Index(name = "idx_pendingconfirmation_to_container_id", columnList = "messagecontainer_id, confirmingmessageuri", unique = true))
public class PendingConfirmation {
    @Id
    @GeneratedValue
    @Column(name = "id")
    protected Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "messagecontainer_id", nullable = false)
    private MessageContainer messageContainer;
    @Column(name = "confirmingMessageURI", nullable = false)
    @Convert(converter = URIConverter.class)
    private URI confirmingMessageURI;
    @Convert(converter = URISetConverter.class)
    @Lob
    private Set<URI> confirmedMessageURI = new HashSet<>();

    public PendingConfirmation() {
    }

    public PendingConfirmation(MessageContainer messageContainer, URI confirmingMessageURI,
                    Set<URI> confirmedMessageURI) {
        super();
        this.messageContainer = messageContainer;
        this.confirmingMessageURI = confirmingMessageURI;
        this.confirmedMessageURI = confirmedMessageURI;
    }

    public URI getConfirmingMessageURI() {
        return confirmingMessageURI;
    }

    public void setConfirmingMessageURI(URI confirmingMessageURI) {
        this.confirmingMessageURI = confirmingMessageURI;
    }

    public Set<URI> getConfirmedMessageURIs() {
        return confirmedMessageURI;
    }

    public void setConfirmedMessageURIs(Set<URI> confirmedMessageURIs) {
        this.confirmedMessageURI = confirmedMessageURIs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((confirmingMessageURI == null) ? 0 : confirmingMessageURI.hashCode());
        result = prime * result + ((messageContainer == null) ? 0 : messageContainer.hashCode());
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
        PendingConfirmation other = (PendingConfirmation) obj;
        if (confirmingMessageURI == null) {
            if (other.confirmingMessageURI != null)
                return false;
        } else if (!confirmingMessageURI.equals(other.confirmingMessageURI))
            return false;
        if (messageContainer == null) {
            if (other.messageContainer != null)
                return false;
        } else if (!messageContainer.equals(other.messageContainer))
            return false;
        return true;
    }
}
