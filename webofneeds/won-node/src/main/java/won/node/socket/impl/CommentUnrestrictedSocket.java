package won.node.socket.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.message.WonMessage;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.SIOC;

import java.lang.invoke.MethodHandles;

/**
 * User: gabriel Date: 17/01/14
 */
public class CommentUnrestrictedSocket extends AbstractSocket {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public SocketType getSocketType() {
        return SocketType.CommentUnrestrictedSocket;
    }

    @Override
    public void connectFromAtom(Connection con, Model content, WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        super.connectFromAtom(con, content, wonMessage);
        /* send a connect back */
        try {
            // TODO: use new system
            // atomFacingConnectionClient.open(con, content, null);
            Atom atom = atomRepository.findOneByAtomURI(con.getAtomURI());
            Model atomContent = atom.getDatatsetHolder().getDataset().getDefaultModel();
            PrefixMapping prefixMapping = PrefixMapping.Factory.create();
            // prefixMapping.setNsPrefix(SIOC.getURI(),"sioc");
            atomContent.withDefaultMappings(prefixMapping);
            atomContent.setNsPrefix("sioc", SIOC.getURI());
            Resource post = atomContent.createResource(con.getAtomURI().toString(), SIOC.POST);
            Resource reply = atomContent.createResource(con.getTargetAtomURI().toString(), SIOC.POST);
            atomContent.add(atomContent.createStatement(atomContent.getResource(con.getAtomURI().toString()),
                            SIOC.HAS_REPLY, atomContent.getResource(con.getTargetAtomURI().toString())));
            // add WON node link
            logger.debug("linked data:" + RdfUtils.toString(atomContent));
            atom.getDatatsetHolder().getDataset().setDefaultModel(atomContent);
            atomRepository.save(atom);
            // } catch (NoSuchConnectionException e) {
            // e.printStackTrace();
            // } catch (IllegalMessageForConnectionStateException e) {
            // e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        }
        /* when connected change linked data */
    }
}
