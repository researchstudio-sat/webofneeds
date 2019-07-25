package won.node.socket.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.SIOC;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: gabriel Date: 17/01/14
 */
public class CommentSocket extends AbstractSocket {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public SocketType getSocketType() {
        return SocketType.CommentSocket;
    }

    @Override
    public void closeFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        super.closeFromAtom(con, content, wonMessage);
        removeDataManagedBySocket(con);
    }

    @Override
    public void closeFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        super.closeFromOwner(con, content, wonMessage);
        removeDataManagedBySocket(con);
    }

    @Override
    public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        super.openFromOwner(con, content, wonMessage);
        addDataManagedBySocket(con);
    }

    @Override
    public void openFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        super.openFromAtom(con, content, wonMessage);
        addDataManagedBySocket(con);
    }

    private void addDataManagedBySocket(final Connection con) {
        Atom atom = atomRepository.findOneByAtomURI(con.getAtomURI());
        Dataset atomContent = atom.getDatatsetHolder().getDataset();
        Model socketManagedGraph = getSocketManagedGraph(con.getAtomURI(), atomContent);
        List<URI> properties = new ArrayList<>();
        PrefixMapping prefixMapping = PrefixMapping.Factory.create();
        // prefixMapping.setNsPrefix(SIOC.getURI(),"sioc");
        socketManagedGraph.withDefaultMappings(prefixMapping);
        socketManagedGraph.setNsPrefix("sioc", SIOC.getURI());
        Resource post = socketManagedGraph.createResource(con.getAtomURI().toString(), SIOC.POST);
        Resource reply = socketManagedGraph.createResource(con.getTargetAtomURI().toString(), SIOC.POST);
        socketManagedGraph.add(socketManagedGraph.createStatement(
                        socketManagedGraph.getResource(con.getAtomURI().toString()), SIOC.HAS_REPLY,
                        socketManagedGraph.getResource(con.getTargetAtomURI().toString())));
        // add WON node link
        logger.debug("linked data:" + RdfUtils.toString(socketManagedGraph));
        atom.getDatatsetHolder().setDataset(atomContent);
        atomRepository.save(atom);
    }

    private void removeDataManagedBySocket(final Connection con) {
        Atom atom = atomRepository.findOneByAtomURI(con.getAtomURI());
        Dataset atomContent = atom.getDatatsetHolder().getDataset();
        removeSocketManagedGraph(con.getAtomURI(), atomContent);
        atom.getDatatsetHolder().setDataset(atomContent);
        atomRepository.save(atom);
    }
}
