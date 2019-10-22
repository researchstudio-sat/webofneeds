package won.protocol.model;

import java.net.URI;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlTransient;

import won.protocol.model.parentaware.ParentAware;

@Entity
@Table(name = "message_in_container", indexes = {
                // indices for this class have the name prefix "IDX_ME"
                @Index(name = "IDX_MIC_PARENT_URI", columnList = "parentURI"),
}, uniqueConstraints = {
                @UniqueConstraint(name = "IDX_MIC_UNIQUE_MESSAGE_IN_CONTAINER", columnNames = { "messageURI",
                                "parentURI" })
})
public class MessageInContainer implements ParentAware<MessageContainer> {
    @Id
    @GeneratedValue
    @Column(name = "id")
    @Convert(converter = URIConverter.class)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "messagecontainer_id", nullable = false)
    private MessageContainer messageContainer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEvent message;
    @Column(name = "messageURI", nullable = false)
    @Convert(converter = URIConverter.class)
    private URI messageURI;
    @Column(name = "parentURI", nullable = false)
    @Convert(converter = URIConverter.class)
    private URI parentURI;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creationDate", nullable = false)
    private Date creationDate;

    public MessageInContainer() {
    }

    public MessageInContainer(MessageEvent message, MessageContainer messageContainer) {
        this.parentURI = messageContainer.getParentUri();
        this.messageURI = message.getMessageURI();
        this.message = message;
        this.messageContainer = messageContainer;
        this.creationDate = new Date();
    }

    @Override
    public MessageContainer getParent() {
        return getMessageContainer();
    }

    public MessageContainer getMessageContainer() {
        return messageContainer;
    }

    public MessageEvent getMessage() {
        return message;
    }

    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public URI getMessageURI() {
        return messageURI;
    }

    public void setMessageURI(final URI messageURI) {
        this.messageURI = messageURI;
    }

    public URI getParentURI() {
        return parentURI;
    }

    public void setParentURI(final URI parentURI) {
        this.parentURI = parentURI;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((messageURI == null) ? 0 : messageURI.hashCode());
        result = prime * result + ((parentURI == null) ? 0 : parentURI.hashCode());
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
        MessageInContainer other = (MessageInContainer) obj;
        if (messageURI == null) {
            if (other.messageURI != null)
                return false;
        } else if (!messageURI.equals(other.messageURI))
            return false;
        if (parentURI == null) {
            if (other.parentURI != null)
                return false;
        } else if (!parentURI.equals(other.parentURI))
            return false;
        return true;
    }
}
