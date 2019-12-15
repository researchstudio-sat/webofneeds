package won.protocol.util.linkeddata;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpHeaders;

/**
 * Encapsulates the response of an LDP container, holding the actual result as
 * well as next and prev links.
 * 
 * @author fkleedorfer
 */
public class LDPContainerPage<T> {
    Optional<URI> nextPageLink = Optional.empty();
    Optional<URI> prevPageLink = Optional.empty();
    T content = null;

    public LDPContainerPage(T content, URI nextPageLink, URI prevPageLink) {
        super();
        Objects.requireNonNull(content);
        this.nextPageLink = Optional.ofNullable(nextPageLink);
        this.prevPageLink = Optional.ofNullable(prevPageLink);
        this.content = content;
    }

    public LDPContainerPage(T content, HttpHeaders responseHeaders) {
        this.content = content;
        this.nextPageLink = WonLinkedDataUtils
                        .extractLDPNextPageLinkFromLinkHeaders(responseHeaders.get(HttpHeaders.LINK));
        this.prevPageLink = WonLinkedDataUtils
                        .extractLDPPrevPageLinkFromLinkHeaders(responseHeaders.get(HttpHeaders.LINK));
    }

    public T getContent() {
        return content;
    }

    public Optional<URI> getNextPageLink() {
        return nextPageLink;
    }

    public Optional<URI> getPrevPageLink() {
        return prevPageLink;
    }
}
