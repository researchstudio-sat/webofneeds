package won.protocol.model;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.List;

/**
 * User: sbyim Date: 11.11.13
 */
@Entity
@Table(name = "ownerApplication")
public class OwnerApplication {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @ManyToMany(mappedBy = "authorizedApplications", targetEntity = Need.class, fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
  private List<Need> needs;

  @ElementCollection(fetch = FetchType.EAGER) // required eager as the object is passed out of a hibernate session in
  // OwnerProtocolOutgoingMessagesProcessor
  @Fetch(value = FetchMode.SUBSELECT)
  @CollectionTable(name = "QueueNames", joinColumns = @JoinColumn(name = "owner_application_id"))
  @Column(name = "queueName")
  private List<String> queueNames;

  @Column(name = "incomingEndpoint")
  private String incomingEndpoint;

  @Column(name = "ownerApplicationId", unique = true)
  private String ownerApplicationId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOwnerApplicationId() {
    return ownerApplicationId;
  }

  public void setOwnerApplicationId(String ownerApplicationId) {
    this.ownerApplicationId = ownerApplicationId;
  }

  // public List<Need> getNeeds() {
  // return needs;
  // }
  //
  // public void setNeeds(List<Need> needs) {
  // this.needs = needs;
  // }

  public List<String> getQueueNames() {
    return queueNames;
  }

  public void setQueueNames(List<String> queueNames) {
    this.queueNames = queueNames;
  }

  public void setIncomingEndpoint(String incomingEndpoint) {

  }
}
