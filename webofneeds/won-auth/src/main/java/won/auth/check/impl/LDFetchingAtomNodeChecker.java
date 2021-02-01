package won.auth.check.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.service.WonNodeInfo;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;
import java.util.Optional;

@Component
public class LDFetchingAtomNodeChecker implements won.auth.check.AtomNodeChecker {
    @Autowired
    LinkedDataSource linkedDataSource;
    URI webIdForRequests;

    public void setWebIdForRequests(URI webIdForRequests) {
        this.webIdForRequests = webIdForRequests;
    }

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    @Override
    public boolean isNodeOfAtom(URI atom, URI node) {
        Optional<WonNodeInfo> wni = WonLinkedDataUtils
                        .findWonNode(atom, Optional.ofNullable(webIdForRequests), linkedDataSource);
        if (wni.isEmpty()) {
            return false;
        }
        String wonNodeURI = wni.get().getWonNodeURI();
        if (wonNodeURI == null) {
            return false;
        }
        return wonNodeURI.equals(node.toString());
    }

    @Override
    public Optional<URI> getNodeOfAtom(URI atom) {
        Optional<WonNodeInfo> wni = WonLinkedDataUtils
                        .findWonNode(atom, Optional.ofNullable(webIdForRequests), linkedDataSource);
        if (wni.isEmpty()) {
            return Optional.empty();
        }
        String wonNodeURI = wni.get().getWonNodeURI();
        return Optional.of(URI.create(wonNodeURI));
    }
}
