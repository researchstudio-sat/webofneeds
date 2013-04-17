package won.protocol.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.model.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 09.04.13
 * Time: 15:36
 * To change this template use File | Settings | File Templates.
 */
public class NeedModelMapper implements ModelMapper<Need> {

    @Override
    public Model toModel(Need need) {
        Model model = ModelFactory.createDefaultModel();

        Resource needNode = model.createResource(need.getNeedURI().toString());

        model.add(model.createStatement(needNode, WON.IS_IN_STATE, need.getState().name()));
        //TODO: check this owner triple
        model.add(model.createStatement(needNode, WON.HAS_OWNER, need.getOwnerURI().toString()));

        Resource connectionsContainer =  model.createResource(need.getNeedURI() + "/connections/");
        model.add(model.createStatement(connectionsContainer, RDF.type, LDP.CONTAINER));

        model.add(model.createStatement(needNode, WON.HAS_CONNECTIONS, connectionsContainer));

        return model;
    }

    @Override
    public Need fromModel(Model model) {
        Need n = new Need();
        Resource rNeed = model.listSubjectsWithProperty(WON.NEED_STATE).nextResource();

        //TODO: Not safe
        n.setNeedURI(URI.create(rNeed.getURI()));

        //TODO: Not safe
        if(model.listObjectsOfProperty(rNeed, WON.NEED_STATE).next().asLiteral().getString().equals(NeedState.ACTIVE.name()))
            n.setState(NeedState.ACTIVE);
        else if(model.listObjectsOfProperty(rNeed, WON.NEED_STATE).next().asLiteral().getString().equals(NeedState.INACTIVE.name()))
            n.setState(NeedState.INACTIVE);

        return n;
    }
}
