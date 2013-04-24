package won.protocol.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import won.protocol.model.BasicNeedType;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.GEO;
import won.protocol.vocabulary.GRDeliveryMethod;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.security.acl.Owner;
import java.text.DateFormat;
import java.util.Date;

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
    Model model = ModelFactory.createDefaultModel();
    Resource needResource = model.createResource(need.getNeedURI().toString(), WON.NEED);
    model.add(model.createStatement(needResource, WON.NEED_CREATION_DATE, DateTimeUtils.format(need.getCreationDate())));

    Resource stateRes = model.createResource(WON.toResource(need.getState()));
    model.add(model.createStatement(needResource, WON.IS_IN_STATE, stateRes));

    Resource ownerRes = model.createResource(need.getOwnerURI().toString());
    model.add(model.createStatement(needResource, WON.HAS_OWNER, ownerRes));

    return model;
  }

  @Override
  public Need fromModel(Model model)
  {
    Need need = new Need();

    Resource needRes = model.getResource(WON.NEED.toString());

    need.setNeedURI(URI.create(needRes.getURI()));

    String dateTime = needRes.getProperty(WON.NEED_CREATION_DATE).getString();
    need.setCreationDate(DateTimeUtils.parse(dateTime));

    Statement stateStat = needRes.getProperty(WON.IS_IN_STATE);
    if (stateStat != null) {
      URI uri = URI.create(stateStat.getResource().getURI());
      need.setState(NeedState.parseString(uri.getFragment()));
    }

    Statement ownerStat = needRes.getProperty(WON.HAS_OWNER);
    if(ownerStat != null) {
      URI uri = URI.create(ownerStat.getResource().getURI());
      need.setOwnerURI(uri);
    }

    return need;
  }
}
