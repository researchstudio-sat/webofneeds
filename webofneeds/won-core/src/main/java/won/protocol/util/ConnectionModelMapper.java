package won.protocol.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import won.protocol.model.Connection;
import won.protocol.model.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 09.04.13
 * Time: 15:38
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionModelMapper implements ModelMapper<Connection> {
    public static final ConnectionModelMapper instance = new ConnectionModelMapper();

    @Override
    public Model toModel(Connection tobject) {
        Model model = ModelFactory.createDefaultModel();

        Resource r = model.createResource(tobject.getConnectionURI().toString());
        r.addProperty(WON.NEED_STATE, tobject.getState().name());
        if(tobject.getRemoteConnectionURI() != null)
            r.addProperty(WON.HAS_REMOTE_CONNECTION, model.createResource(tobject.getRemoteConnectionURI().toString()));
        r.addProperty(WON.REMOTE_NEED, model.createResource(tobject.getRemoteNeedURI().toString()));
        r.addProperty(WON.BELONGS_TO_NEED, model.createResource(tobject.getNeedURI().toString()));

        return model;
    }

    @Override
    public Connection fromModel(Model model) {
        Connection c = new Connection();
        Resource rCon = model.listSubjectsWithProperty(WON.NEED_STATE).nextResource();

        //TODO: Not Safe
        c.setConnectionURI(URI.create(rCon.getURI()));
        c.setNeedURI(URI.create(rCon.listProperties(WON.BELONGS_TO_NEED).next().getString()));
        c.setRemoteNeedURI(URI.create(rCon.listProperties(WON.REMOTE_NEED).next().getString()));

        StmtIterator itRemoteCon = rCon.listProperties(WON.HAS_REMOTE_CONNECTION);
        if(itRemoteCon.hasNext())
           c.setRemoteConnectionURI(URI.create(itRemoteCon.next().getString()));

        return c;
    }
}
