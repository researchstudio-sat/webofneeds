package won.bot.framework.extensions.serviceatom;

import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXHOLD;
import won.protocol.vocabulary.WXSCHEMA;

import java.net.URI;
import java.util.*;

public class ServiceAtomContent {
    private String name;
    private String description;
    private String termsOfService;
    private Collection<String> tags;
    private Collection<URI> flags = new ArrayList<>();
    private Map<String, String> sockets;

    public ServiceAtomContent(String name) {
        this(name, null);
    }

    public ServiceAtomContent(String name, Map<String, String> sockets) {
        this.name = name;
        if (sockets == null) {
            this.sockets = new HashMap<>();
            this.sockets.put("#HolderSocket", WXHOLD.HolderSocketString);
            this.sockets.put("#ChatSocket", WXCHAT.ChatSocketString);
            this.sockets.put("#sReviewSocket", WXSCHEMA.ReviewSocketString);
        } else {
            this.sockets = sockets;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public void setTags(Collection<String> tags) {
        this.tags = tags;
    }

    public Collection<URI> getFlags() {
        return flags;
    }

    public void setFlags(Collection<URI> flags) {
        this.flags = flags;
    }

    public void setSockets(Map<String, String> sockets) {
        this.sockets = sockets;
    }

    public Map<String, String> getSockets() {
        return sockets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ServiceAtomContent that = (ServiceAtomContent) o;
        if (that.sockets != null && sockets != null) {
            if (that.sockets.size() != sockets.size()) {
                return false;
            } else {
                for (String socketTypeUri : sockets.values()) {
                    if (!that.sockets.containsValue(socketTypeUri)) {
                        return false;
                    }
                }
            }
        } else if (that.sockets != sockets) {
            return false;
        }
        return name.equals(that.name) &&
                        Objects.equals(description, that.description) &&
                        Objects.equals(termsOfService, that.termsOfService) &&
                        ((tags == null && that.tags == null) || tags != null && tags.containsAll(that.tags)
                                        && that.tags != null && that.tags.containsAll(tags))
                        &&
                        ((flags == null && that.flags == null) || flags != null && flags.containsAll(that.flags)
                                        && that.flags != null && that.flags.containsAll(flags));
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, termsOfService, tags);
    }
}
