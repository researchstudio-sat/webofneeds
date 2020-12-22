package won.auth.check;

import java.net.URI;
import java.util.Optional;

public interface AtomNodeChecker {
    boolean isNodeOfAtom(URI atom, URI node);

    Optional<URI> getNodeOfAtom(URI atom);
}
