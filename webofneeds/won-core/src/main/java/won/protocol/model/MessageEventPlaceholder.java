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
import javax.persistence.OneToOne;
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
    @Index(name = "IDX_ME_INNERMOST_MESSAGE_URI_RECEIVER_NEED_URI", columnList = "messageURI, receiverNeedURI, innermostMessageURI, correspondingRemoteMessageURI") }, uniqueConstraints = {
        @UniqueConstraint(name = "IDX_ME_UNIQUE_MESSAGE_URI", columnNames = "messageURI"),
        @UniqueConstraint(name = "IDX_ME_UNIQUE_CORREXPONDING_REMOTE_MESSAGE_URI", columnNames = "correspondingRemoteMessageURI"),
        @UniqueConstraint(name = "IDX_ME_UNIQUE_DATASETHOLDER_ID", columnNames = "datasetholder_id") })
public class MessageEventPlaceholder implements ParentAware<EventContainer> {

  public MessageEventPlaceholder() {
  }

  public MessageEventPlaceholder(URI parentURI, WonMessage wonMessage, EventContainer eventContainer) {
    this.parentURI = parentURI;
    this.messageURI = wonMessage.getMessageURI();
    this.messageType = wonMessage.getMessageType();
    this.senderURI = wonMessage.getSenderURI();
    this.senderNeedURI = wonMessage.getSenderNeedURI();
    this.senderNodeURI = wonMessage.getSenderNodeURI();
    this.receiverURI = wonMessage.getReceiverURI();
    this.receiverNeedURI = wonMessage.getReceiverNeedURI();
    this.receiverNodeURI = wonMessage.getReceiverNodeURI();
    this.creationDate = new Date();
    this.correspondingRemoteMessageURI = wonMessage.getCorrespondingRemoteMessageURI();
    this.referencedByOtherMessage = false;
    this.innermostMessageURI = wonMessage.getInnermostMessageURI();
    this.eventContainer = eventContainer;
  }

  @Id
  @GeneratedValue
  @Column(name = "id")
  @Convert(converter = URIConverter.class)
  private Long id;

  @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
  private int version = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "eventcontainer_id")
  private EventContainer eventContainer;

  @Column(name = "messageURI")
  @Convert(converter = URIConverter.class)
  private URI messageURI;

  // this URI refers to the need (in case of create, de-/activate) or connection
  // (in case of hint, open,
  // close, etc.)
  @Column(name = "parentURI")
  @Convert(converter = URIConverter.class)
  private URI parentURI;

  @Column(name = "messageType")
  @Enumerated(EnumType.STRING)
  private WonMessageType messageType; // ConnectMessage, CreateMessage, NeedStateMessage

  @Column(name = "senderURI")
  @Convert(converter = URIConverter.class)
  private URI senderURI;

  @Column(name = "senderNeedURI")
  @Convert(converter = URIConverter.class)
  private URI senderNeedURI;

  @Column(name = "senderNodeURI")
  @Convert(converter = URIConverter.class)
  private URI senderNodeURI;

  @Column(name = "receiverURI")
  @Convert(converter = URIConverter.class)
  private URI receiverURI;

  @Column(name = "receiverNeedURI")
  @Convert(converter = URIConverter.class)
  private URI receiverNeedURI;

  @Column(name = "receiverNodeURI")
  @Convert(converter = URIConverter.class)
  private URI receiverNodeURI;

  @Column(name = "creationDate")
  private Date creationDate;

  @Column(name = "correspondingRemoteMessageURI")
  @Convert(converter = URIConverter.class)
  private URI correspondingRemoteMessageURI;

  @Column(name = "responseMessageURI")
  @Convert(converter = URIConverter.class)
  private URI responseMessageURI;

  @Column(name = "referencedByOtherMessage")
  private boolean referencedByOtherMessage;

  @Column(name = "innermostMessageURI")
  @Convert(converter = URIConverter.class)
  private URI innermostMessageURI;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private DatasetHolder datasetHolder;

  @PreUpdate
  @PrePersist
  public void incrementVersion() {
    this.version++;
  }

  @Override
  public EventContainer getParent() {
    return getEventContainer();
  }

  public EventContainer getEventContainer() {
    return eventContainer;
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

  public URI getSenderNeedURI() {
    return senderNeedURI;
  }

  public void setSenderNeedURI(final URI senderNeedURI) {
    this.senderNeedURI = senderNeedURI;
  }

  public URI getSenderNodeURI() {
    return senderNodeURI;
  }

  public void setSenderNodeURI(final URI senderNodeURI) {
    this.senderNodeURI = senderNodeURI;
  }

  public URI getReceiverURI() {
    return receiverURI;
  }

  public void setReceiverURI(final URI receiverURI) {
    this.receiverURI = receiverURI;
  }

  public URI getReceiverNeedURI() {
    return receiverNeedURI;
  }

  public void setReceiverNeedURI(final URI receiverNeedURI) {
    this.receiverNeedURI = receiverNeedURI;
  }

  public URI getReceiverNodeURI() {
    return receiverNodeURI;
  }

  public void setReceiverNodeURI(final URI receiverNodeURI) {
    this.receiverNodeURI = receiverNodeURI;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(final Date creationDate) {
    this.creationDate = creationDate;
  }

  public URI getCorrespondingRemoteMessageURI() {
    return correspondingRemoteMessageURI;
  }

  public void setCorrespondingRemoteMessageURI(final URI correspondingRemoteMessageURI) {
    this.correspondingRemoteMessageURI = correspondingRemoteMessageURI;
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

  public URI getInnermostMessageURI() {
    return innermostMessageURI;
  }

  public void setInnermostMessageURI(URI innermostMessageURI) {
    this.innermostMessageURI = innermostMessageURI;
  }

  public DatasetHolder getDatasetHolder() {
    return datasetHolder;
  }

  public void setDatasetHolder(final DatasetHolder datasetHolder) {
    this.datasetHolder = datasetHolder;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (!(o instanceof MessageEventPlaceholder))
      return false;

    final MessageEventPlaceholder that = (MessageEventPlaceholder) o;

    if (messageType != null ? !messageType.equals(that.messageType) : that.messageType != null)
      return false;
    if (messageURI != null ? !messageURI.equals(that.messageURI) : that.messageURI != null)
      return false;
    if (receiverURI != null ? !receiverURI.equals(that.receiverURI) : that.receiverURI != null)
      return false;
    if (senderURI != null ? !senderURI.equals(that.senderURI) : that.senderURI != null)
      return false;
//    if (signatures != null ? !signatures.equals(that.signatures) : that.signatures != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = messageURI != null ? messageURI.hashCode() : 0;
    result = 31 * result + (messageType != null ? messageType.hashCode() : 0);
    result = 31 * result + (senderURI != null ? senderURI.hashCode() : 0);
    result = 31 * result + (receiverURI != null ? receiverURI.hashCode() : 0);
    return result;
  }

}
