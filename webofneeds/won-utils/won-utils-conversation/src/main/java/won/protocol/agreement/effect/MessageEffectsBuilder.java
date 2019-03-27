package won.protocol.agreement.effect;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class MessageEffectsBuilder {
    private final URI messageUri;
    private Optional<Proposes> proposesOptional = Optional.empty();
    private Set<Accepts> accepts = new HashSet<>();
    private Set<Retracts> retracts = new HashSet<>();
    private Set<Rejects> rejects = new HashSet<>();
    private Set<Claims> claims = new HashSet<>();

    public MessageEffectsBuilder(URI messageUri) {
        this.messageUri = messageUri;
    }

    public Set<MessageEffect> build() {
        Set<MessageEffect> ret = new HashSet<MessageEffect>();
        if (proposesOptional.isPresent()) {
            ret.add(proposesOptional.get());
        }
        ret.addAll(accepts);
        ret.addAll(rejects);
        ret.addAll(retracts);
        ret.addAll(claims);
        return ret;
    }

    public MessageEffectsBuilder proposes(URI uri) {
        if (!proposesOptional.isPresent()) {
            proposesOptional = Optional.of(new Proposes(this.messageUri));
        }
        proposesOptional.get().addProposes(uri);
        return this;
    }

    public MessageEffectsBuilder proposesToCancel(URI uri) {
        if (!proposesOptional.isPresent()) {
            proposesOptional = Optional.of(new Proposes(this.messageUri));
        }
        proposesOptional.get().addProposesToCancel(uri);
        return this;
    }

    public MessageEffectsBuilder accepts(URI uri, Collection<URI> cancelled) {
        this.accepts.add(new Accepts(messageUri, uri, cancelled));
        return this;
    }

    public MessageEffectsBuilder rejects(URI uri) {
        this.rejects.add(new Rejects(messageUri, uri));
        return this;
    }

    public MessageEffectsBuilder retracts(URI uri) {
        this.retracts.add(new Retracts(messageUri, uri));
        return this;
    }

    public MessageEffectsBuilder claims(URI uri) {
        this.claims.add(new Claims(messageUri, uri));
        return this;
    }
}
