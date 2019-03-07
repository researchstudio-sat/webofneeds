package won.node.facet.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.Need;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.SIOC;

/**
 * User: gabriel
 * Date: 17/01/14
 */
public class CommentUnrestrictedFacet extends AbstractFacet
{
  private Logger logger = LoggerFactory.getLogger(this.getClass());
  @Override
  public FacetType getFacetType() {
    return FacetType.CommentUnrestrictedFacet;
  }

    @Override
  public void connectFromNeed(Connection con, Model content, WonMessage wonMessage)
            throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    super.connectFromNeed(con, content, wonMessage);
    /* send a connect back */
    try {
     //TODO: use new system
     // needFacingConnectionClient.open(con, content, null);
      Need need = needRepository.findOneByNeedURI(con.getNeedURI());
      Model needContent = need.getDatatsetHolder().getDataset().getDefaultModel();
      PrefixMapping prefixMapping = PrefixMapping.Factory.create();
//    prefixMapping.setNsPrefix(SIOC.getURI(),"sioc");
      needContent.withDefaultMappings(prefixMapping);
      needContent.setNsPrefix("sioc", SIOC.getURI());
      Resource post = needContent.createResource(con.getNeedURI().toString(), SIOC.POST);
      Resource reply = needContent.createResource(con.getRemoteNeedURI().toString(),SIOC.POST);
      needContent.add(needContent.createStatement(needContent.getResource(con.getNeedURI().toString()), SIOC.HAS_REPLY,
                                                  needContent.getResource(con.getRemoteNeedURI().toString())));

      // add WON node link
      logger.debug("linked data:"+ RdfUtils.toString(needContent));
      need.getDatatsetHolder().getDataset().setDefaultModel(needContent);
      needRepository.save(need);
//    } catch (NoSuchConnectionException e) {
//      e.printStackTrace();
//    } catch (IllegalMessageForConnectionStateException e) {
//      e.printStackTrace();
    } catch (Exception e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    /* when connected change linked data*/
  }
}
