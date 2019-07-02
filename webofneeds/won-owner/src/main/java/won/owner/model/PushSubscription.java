package won.owner.model;

import nl.martijndwars.webpush.Subscription;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "pushSubscriptions", uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "endpoint" }) })
public class PushSubscription {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
    @Column(nullable = false)
    private String endpoint;
    @Column(nullable = false)
    private String key;
    @Column(nullable = false)
    private String auth;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updated;
    @ManyToOne(targetEntity = User.class)
    @JoinColumn(nullable = false, name = "user_id")
    private User owner;

    public PushSubscription() {
    }

    @PrePersist
    protected void onCreate() {
        updated = new Date();
    }

    protected void updateDate() {
        updated = new Date();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public PushSubscription(User owner, Subscription subscription) {
        this.owner = owner;
        this.endpoint = subscription.endpoint;
        this.key = subscription.keys.p256dh;
        this.auth = subscription.keys.auth;
    }

    public Subscription toSubscription() {
        return new Subscription(endpoint, new Subscription.Keys(key, auth));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PushSubscription that = (PushSubscription) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
