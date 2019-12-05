package won.protocol.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

public class URICount implements Serializable {
    private static final long serialVersionUID = -4952062106426604066L;
    private URI uri;
    private int count;

    public URICount(URI uri, int count) {
        super();
        Objects.requireNonNull(uri);
        this.uri = uri;
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public URI getUri() {
        return uri;
    }

    public URICount increment() {
        return new URICount(uri, count + 1);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + count;
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
        URICount other = (URICount) obj;
        if (count != other.count)
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "URICount [uri=" + uri + ", count=" + count + "]";
    }
}
