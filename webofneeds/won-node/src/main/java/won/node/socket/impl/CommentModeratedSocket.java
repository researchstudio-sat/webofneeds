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
import won.protocol.model.Connection;
import won.protocol.model.SocketType;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.SIOC;

import java.lang.invoke.MethodHandles;

/**
 * User: gabriel Date: 17/01/14
 */
public class CommentModeratedSocket extends AbstractSocket {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public SocketType getSocketType() {
        return SocketType.CommentModeratedSocket;
    }

    @Override
    public void connectFromOwner(Connection con, Model content, WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        super.connectFromOwner(con, content, wonMessage);
        /* when connected change linked data */
        PrefixMapping prefixMapping = PrefixMapping.Factory.create();
        prefixMapping.setNsPrefix(SIOC.getURI(), "sioc");
        content.withDefaultMappings(prefixMapping);
        content.setNsPrefix("sioc", SIOC.getURI());
        Resource post = content.createResource(con.getConnectionURI() + "/p/", SIOC.POST);
        content.add(content.createStatement(content.getResource(con.getConnectionURI().toString()), SIOC.HAS_REPLY,
                        content.getResource(con.getTargetConnectionURI().toString())));
        logger.debug(RdfUtils.toString(content));
        con.getDatasetHolder().getDataset().setDefaultModel(content);
        datasetHolderRepository.save(con.getDatasetHolder());
    }
}
