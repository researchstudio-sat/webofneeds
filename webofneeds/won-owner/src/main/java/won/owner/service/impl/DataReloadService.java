package won.owner.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedReadService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 04.04.13
 * Time: 14:07
 * To change this template use File | Settings | File Templates.
 */
public class DataReloadService {
    private OwnerProtocolNeedReadService ownerService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    public void setOwnerService(OwnerProtocolNeedReadService ownerService) {
        this.ownerService = ownerService;
    }

    public void reload() {
        for (Need n : needRepository.findAll()) {
            reloadNeed(n.getId());
            try {
                for(URI cURI : ownerService.listConnectionURIs(n.getNeedURI())) {
                   reloadConnection(cURI);
                }
            } catch (NoSuchNeedException e) {
                logger.warn("caught NoSuchNeedException:", e);
            }
        }
    }

    public void reloadNeed(long id) {
        Need n = null;

        try {
            n = ownerService.readNeed(needRepository.findById(id).get(0).getNeedURI());
            n.setId(id);
            needRepository.saveAndFlush(n);
        } catch (NoSuchNeedException e) {
            logger.warn("caught NoSuchNeedException:", e);
        }
    }

    public Need importNeed(URI needURI) {
      logger.debug("importing need {}", needURI);
      assert needURI != null : "cannot import - needURI is null";
      Need need = null;
      try {
        need = ownerService.readNeed(needURI);
        needRepository.saveAndFlush(need);
        Collection<URI> connectionURIs = ownerService.listConnectionURIs(needURI);
        logger.debug("importing {} connections", connectionURIs.size());
        for(URI connectionURI: connectionURIs) {
          importConnection(connectionURI);
        }
        return need;
      } catch (NoSuchNeedException e) {
        logger.warn("caught NoSuchNeedException:", e);
      }
      return null;
    }

    public Connection importConnection(URI connectionURI) {
      assert connectionURI != null : "cannot import - connectionURI is null";
      logger.debug("importing connection {}", connectionURI);
      Connection conn = null;
      try {
        logger.debug("reading connection {}", connectionURI);
        conn = ownerService.readConnection(connectionURI);
        if (logger.isDebugEnabled() && conn != null && conn.getNeedURI() != null) {
          logger.debug("import successful for connection {}", connectionURI);
        }
        connectionRepository.saveAndFlush(conn);
        return conn;
      } catch (NoSuchConnectionException e) {
        logger.warn("caught Exception:", e);
      }
      return null;
    }

    public void reloadConnection(URI uri) {
        Connection c = null;

        List<Connection> cons = connectionRepository.findByConnectionURI(uri);

        try {
            switch(cons.size()) {
                case 0:
                    c = ownerService.readConnection(uri);
                    connectionRepository.saveAndFlush(c);
                    break;
                case 1:
                    c = ownerService.readConnection(uri);
                    c.setId(cons.get(0).getId());
                    connectionRepository.saveAndFlush(c);
                    break;
                default:
                    throw new IllegalStateException("Connection-URI is not unique in local Database!");
            }
        } catch (NoSuchConnectionException e) {
            logger.warn("caught NoSuchConnectionException:", e);
        }
    }
}
