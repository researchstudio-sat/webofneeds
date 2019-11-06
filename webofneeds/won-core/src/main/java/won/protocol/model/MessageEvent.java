package won.protocol.model;

import java.net.URI;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlTransient;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.model.parentaware.ParentAware;

@Entity
@Table(name = "message_event", indexes = {
                // indices for this class have the name prefix "IDX_ME"
                @Index(name = "IDX_ME_PARENT_URI", columnList = "parentURI"),
                @Index(name = "IDX_ME_PARENT_URI_MESSAGE_TYPE", columnList = "parentURI, messageType"),
                @Index(name = "IDX_ME_PARENT_URI_REFERENCED_BY_OTHER_MESSAGE", columnList = "parentURI, referencedByOtherMessage"),
                @Index(name = "IDX_ME_RECIPIENT_ATOM_URI", columnList = "messageURI, recipientAtomURI") }, uniqueConstraints = {
                                @UniqueConstraint(name = "IDX_ME_UNIQUE_MESSAGE_URI_PER_PARENT", columnNames = {
                                                "messageURI",
                                                "parentURI" }) })
public class MessageEvent implements ParentAware<MessageContainer> {
    public MessageEvent() {
    }

    public MessageEvent(URI parentURI, WonMessage wonMessage, MessageContainer messageContainer) {
        this.parentURI = parentURI;
        this.messageURI = wonMessage.getMessageURI();
        this.messageType = wonMessage.getMessageType();
        this.senderURI = wonMessage.getSenderURI();
        this.senderAtomURI = wonMessage.getSenderAtomURI();
        this.senderNodeURI = wonMessage.getSenderNodeURI();
        this.recipientURI = wonMessage.getRecipientURI();
        this.recipientAtomURI = wonMessage.getRecipientAtomURI();
        this.recipientNodeURI = wonMessage.getRecipientNodeURI();
        this.creationDate = new Date();
        this.referencedByOtherMessage = false;
        this.messageContainer = messageContainer;
    }

    @Id
    @GeneratedValue
    @Column(name = "id")
    @Convert(converter = URIConverter.class)
    private Long id;
    @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
    private int version = 0;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "messagecontainer_id")
    private MessageContainer messageContainer;
    @Column(name = "messageURI")
    @Convert(converter = URIConverter.class)
    private URI messageURI;
    // this URI refers to the atom (in case of create, de-/activate) or connection
    // (in case of hint, open,
    // close, etc.)
    @Column(name = "parentURI")
    @Convert(converter = URIConverter.class)
    private URI parentURI;
    @Column(name = "messageType")
    @Enumerated(EnumType.STRING)
    private WonMessageType messageType; // ConnectMessage, CreateMessage, AtomStateMessage
    @Column(name = "senderURI")
    @Convert(converter = URIConverter.class)
    private URI senderURI;
    @Column(name = "senderAtomURI")
    @Convert(converter = URIConverter.class)
    private URI senderAtomURI;
    @Column(name = "senderNodeURI")
    @Convert(converter = URIConverter.class)
    private URI senderNodeURI;
    @Column(name = "recipientURI")
    @Convert(converter = URIConverter.class)
    private URI recipientURI;
    @Column(name = "recipientAtomURI")
    @Convert(converter = URIConverter.class)
    private URI recipientAtomURI;
    @Column(name = "recipientNodeURI")
    @Convert(converter = URIConverter.class)
    private URI recipientNodeURI;
    @Column(name = "creationDate")
    private Date creationDate;
    @Column(name = "responseMessageURI")
    @Convert(converter = URIConverter.class)
    private URI responseMessageURI;
    @Column(name = "referencedByOtherMessage")
    private boolean referencedByOtherMessage;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private DatasetHolder datasetHolder;

    @PreUpdate
    @PrePersist
    public void incrementVersion() {
        this.version++;
    }

    @Override
    public MessageContainer getParent() {
        return getMessageContainer();
    }

    public MessageContainer getMessageContainer() {
        return messageContainer;
    }

    public int getVersion() {
        return version;
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

    public WonMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(final WonMessageType messageType) {
        this.messageType = messageType;
    }

    public URI getSenderURI() {
        return senderURI;
    }

    public void setSenderURI(final URI senderURI) {
        this.senderURI = senderURI;
    }

    public URI getSenderAtomURI() {
        return senderAtomURI;
    }

    public void setSenderAtomURI(final URI senderAtomURI) {
        this.senderAtomURI = senderAtomURI;
    }

    public URI getSenderNodeURI() {
        return senderNodeURI;
    }

    public void setSenderNodeURI(final URI senderNodeURI) {
        this.senderNodeURI = senderNodeURI;
    }

    public URI getRecipientURI() {
        return recipientURI;
    }

    public void setRecipientURI(final URI recipientURI) {
        this.recipientURI = recipientURI;
    }

    public URI getRecipientAtomURI() {
        return recipientAtomURI;
    }

    public void setRecipientAtomURI(final URI recipientAtomURI) {
        this.recipientAtomURI = recipientAtomURI;
    }

    public URI getRecipientNodeURI() {
        return recipientNodeURI;
    }

    public void setRecipientNodeURI(final URI recipientNodeURI) {
        this.recipientNodeURI = recipientNodeURI;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isReferencedByOtherMessage() {
        return referencedByOtherMessage;
    }

    public void setReferencedByOtherMessage(final boolean referencedByOtherMessage) {
        this.referencedByOtherMessage = referencedByOtherMessage;
    }

    public URI getResponseMessageURI() {
        return responseMessageURI;
    }

    public void setResponseMessageURI(final URI responseMessageURI) {
        this.responseMessageURI = responseMessageURI;
    }

    public DatasetHolder getDatasetHolder() {
        return datasetHolder;
    }

    public void setDatasetHolder(final DatasetHolder datasetHolder) {
        this.datasetHolder = datasetHolder;
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
        MessageEvent other = (MessageEvent) obj;
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
