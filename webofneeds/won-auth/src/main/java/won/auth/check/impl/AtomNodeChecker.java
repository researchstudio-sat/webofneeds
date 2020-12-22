package won.auth.check.impl;

import java.net.URI;
import java.util.Optional;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Component
public class AtomNodeChecker implements won.auth.check.AtomNodeChecker {
    @Autowired
    LinkedDataSource linkedDataSource;

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    @Override
    public boolean isNodeOfAtom(URI atom, URI node) {
        Dataset atomDataset = WonLinkedDataUtils.getFullAtomDataset(atom, linkedDataSource);
        if (atomDataset == null) {
            return false;
        }
        URI wonNodeURI = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(atomDataset, atom);
        if (wonNodeURI == null) {
            return false;
        }
        return node.equals(wonNodeURI);
    }

    @Override public Optional<URI> getNodeOfAtom(URI atom) {
        Dataset atomDataset = WonLinkedDataUtils.getFullAtomDataset(atom, linkedDataSource);
        if (atomDataset == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(atomDataset, atom));
    }
}
