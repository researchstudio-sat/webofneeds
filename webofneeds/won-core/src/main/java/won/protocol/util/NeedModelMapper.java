package won.protocol.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * User: gabriel
 * Date: 09.04.13
 * Time: 15:36
 */
public class NeedModelMapper implements ModelMapper<Need>
{

  @Override
  public Model toModel(Need need)
  {
    // TODO: see if we can use RDF storage here

    Model model = ModelFactory.createDefaultModel();
    Resource needResource = model.createResource(need.getNeedURI().toString(), WON.NEED);
    model.add(model.createStatement(needResource, WON.IS_IN_STATE, WON.toResource(need.getState())));

    return model;
  }

  @Override
  public Need fromModel(Model model)
  {
    Need n = new Need();
    Resource rNeed = model.listSubjectsWithProperty(WON.NEED_STATE).nextResource();

    //TODO: Not safe
    n.setNeedURI(URI.create(rNeed.getURI()));

    //TODO: Not safe
    if (model.listObjectsOfProperty(rNeed, WON.NEED_STATE).next().asLiteral().getString().equals(NeedState.ACTIVE.name()))
      n.setState(NeedState.ACTIVE);
    else if (model.listObjectsOfProperty(rNeed, WON.NEED_STATE).next().asLiteral().getString().equals(NeedState.INACTIVE.name()))
      n.setState(NeedState.INACTIVE);

    return n;
  }
}
