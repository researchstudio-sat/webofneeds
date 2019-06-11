package won.owner.model;

import nl.martijndwars.webpush.Subscription;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(name = "pushSubscriptions", uniqueConstraints = @UniqueConstraint(columnNames = { "endpoint" }))
public class PushSubscription {
    @Id
    private String endpoint;
    private String key;
    private String auth;

    public PushSubscription() {
    }

    public PushSubscription(Subscription subscription) {
        this.endpoint = subscription.endpoint;
        this.key = subscription.keys.p256dh;
        this.auth = subscription.keys.auth;
    }

    public Subscription toSubscription() {
        return new Subscription(endpoint, new Subscription.Keys(key, auth));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PushSubscription that = (PushSubscription) o;
        return endpoint.equals(that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint);
    }
}
