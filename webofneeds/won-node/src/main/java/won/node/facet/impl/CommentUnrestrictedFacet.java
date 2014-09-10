package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
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
      // ToDo (FS): create open WonMessage
      needFacingConnectionClient.open(con, content, null);
      Model needContent = rdfStorageService.loadModel(con.getNeedURI());
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
      rdfStorageService.storeModel(con.getNeedURI(), needContent);
    } catch (NoSuchConnectionException e) {
      e.printStackTrace();
    } catch (IllegalMessageForConnectionStateException e) {
      e.printStackTrace();
    } catch (Exception e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    /* when connected change linked data*/
  }
}
