package won.node.facet.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.SIOC;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: gabriel
 * Date: 17/01/14
 */
public class CommentFacet extends AbstractFacet
{
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public FacetType getFacetType() {
    return FacetType.CommentFacet;
  }

  @Override
  public void connectFromNeed(final Connection con, final Model content, final Dataset messageEvent)
          throws NoSuchNeedException,
    IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {


    super.connectFromNeed(con, content, messageEvent);
    addLinkedDataStatements(con, content);
    // Model content = rdfStorageService.loadContent(con.getNeedURI());


  }
  @Override
  public void connectFromOwner(final Connection con, final Model content, final Dataset messageEvent)
          throws NoSuchNeedException,
    IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    super.connectFromOwner(con, content, messageEvent);
    addLinkedDataStatements(con, content);
  }
  private void addLinkedDataStatements(final Connection con, final Model content){
    List<URI> properties = new ArrayList<>();

    PrefixMapping prefixMapping = PrefixMapping.Factory.create();
//    prefixMapping.setNsPrefix(SIOC.getURI(),"sioc");
    content.withDefaultMappings(prefixMapping);
    content.setNsPrefix("sioc", SIOC.getURI());
    Resource post = content.createResource(con.getNeedURI().toString(), SIOC.POST);
    Resource reply = content.createResource(con.getRemoteNeedURI().toString(),SIOC.POST);
    content.add(content.createStatement(content.getResource(con.getNeedURI().toString()), SIOC.HAS_REPLY,
                                        content.getResource(con.getRemoteNeedURI().toString())));

    // add WON node link
    logger.debug("linked data:"+ RdfUtils.toString(content));
    rdfStorageService.storeContent(con.getNeedURI(),content);
  }

}
