package won.owner.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import won.owner.exception.DuplicateConnectionException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;

import java.net.URI;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 04.04.13
 * Time: 14:07
 * To change this template use File | Settings | File Templates.
 */
public class DataReloadService {
    private OwnerProtocolNeedService ownerService;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    public void setOwnerService(OwnerProtocolNeedService ownerService) {
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
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (DuplicateConnectionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void reloadConnection(URI uri) throws DuplicateConnectionException {
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
                    throw new DuplicateConnectionException();
            }
        } catch (NoSuchConnectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
