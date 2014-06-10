package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
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
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
public class OwnerFacetImpl extends AbstractFacet
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public FacetType getFacetType() {
      return FacetType.OwnerFacet;
  }

  @Override
  public void connectFromNeed(final Connection con, final Model content) throws NoSuchNeedException,
    IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

    super.connectFromNeed(con, content);
    /* when connected change linked data*/

   // Model content = rdfStorageService.loadContent(con.getNeedURI());
    List<URI> properties = new ArrayList<>();

    NodeIterator nodeIter = content.listObjectsOfProperty(WON.HAS_REMOTE_FACET);
    RDFNode node = nodeIter.next();

    if (node.asResource().getURI().equals(FacetType.CommentFacet)){

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

  @Override
  public void connectFromOwner(final Connection con, final Model content) throws NoSuchNeedException,
    IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    super.connectFromOwner(con, content);
    List<URI> properties = new ArrayList<>();

    NodeIterator nodeIter = content.listObjectsOfProperty(WON.HAS_REMOTE_FACET);
    RDFNode node = nodeIter.next();

    if (URI.create(node.asResource().getURI()).equals(FacetType.CommentFacet.getURI())){

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
}
