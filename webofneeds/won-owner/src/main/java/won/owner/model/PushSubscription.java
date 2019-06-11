package won.owner.model;

import nl.martijndwars.webpush.Subscription;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "pushSubscriptions", uniqueConstraints = @UniqueConstraint(columnNames = { "endpoint" }))
public class PushSubscription {
    @Id
    private String endpoint;
    private String key;
    private String auth;

    public PushSubscription(Subscription subscription) {
        this.endpoint = subscription.endpoint;
        this.key = subscription.keys.p256dh;
        this.auth = subscription.keys.auth;
    }

    public Subscription toSubscription() {
        return new Subscription(endpoint, new Subscription.Keys(key, auth));
    }
}
