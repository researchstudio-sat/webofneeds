package won.node.springsecurity.acl;

import org.springframework.beans.factory.annotation.Autowired;
import won.auth.check.AtomNodeChecker;
import won.auth.check.impl.LDFetchingAtomNodeChecker;
import won.node.service.nodeconfig.URIService;
import won.node.service.persistence.AtomService;

import java.net.URI;
import java.util.Optional;

public class NodeBasedAtomNodeChecker implements AtomNodeChecker {
    @Autowired
    private LDFetchingAtomNodeChecker ldFetchingAtomNodeChecker;
    @Autowired
    private AtomService atomService;
    @Autowired
    private URIService uriService;

    public NodeBasedAtomNodeChecker() {
    }

    @Override
    public boolean isNodeOfAtom(URI atom, URI node) {
        if (uriService.isAtomURI(atom)) {
            return (URI.create(uriService.getResourceURIPrefix()).equals(node));
        }
        return ldFetchingAtomNodeChecker.isNodeOfAtom(atom, node);
    }

    @Override
    public Optional<URI> getNodeOfAtom(URI atom) {
        if (uriService.isAtomURI(atom)) {
            return Optional.of(URI.create(uriService.getResourceURIPrefix()));
        }
        return ldFetchingAtomNodeChecker.getNodeOfAtom(atom);
    }
}
