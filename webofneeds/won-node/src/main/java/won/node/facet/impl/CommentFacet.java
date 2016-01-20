package won.node.facet.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
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
  public void closeFromNeed(final Connection con, final Model content, final WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    super.closeFromNeed(con, content, wonMessage);
    removeDataManagedByFacet(con);
  }

  @Override
  public void closeFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    super.closeFromOwner(con, content, wonMessage);
    removeDataManagedByFacet(con);
  }

  @Override
  public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    super.openFromOwner(con, content, wonMessage);
    addDataManagedByFacet(con);
  }

  @Override
  public void openFromNeed(final Connection con, final Model content, final WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    super.openFromNeed(con, content, wonMessage);
    addDataManagedByFacet(con);
  }

  private void addDataManagedByFacet(final Connection con){
    Dataset needContent = rdfStorageService.loadDataset(con.getNeedURI());
    Model facetManagedGraph = getFacetManagedGraph(con.getNeedURI(), needContent);

    List<URI> properties = new ArrayList<>();

    PrefixMapping prefixMapping = PrefixMapping.Factory.create();
//    prefixMapping.setNsPrefix(SIOC.getURI(),"sioc");
    facetManagedGraph.withDefaultMappings(prefixMapping);
    facetManagedGraph.setNsPrefix("sioc", SIOC.getURI());
    Resource post = facetManagedGraph.createResource(con.getNeedURI().toString(), SIOC.POST);
    Resource reply = facetManagedGraph.createResource(con.getRemoteNeedURI().toString(),SIOC.POST);
    facetManagedGraph.add(facetManagedGraph
      .createStatement(facetManagedGraph.getResource(con.getNeedURI().toString()), SIOC.HAS_REPLY,
        facetManagedGraph.getResource(con.getRemoteNeedURI().toString())));

    // add WON node link
    logger.debug("linked data:"+ RdfUtils.toString(facetManagedGraph));
    rdfStorageService.storeDataset(con.getNeedURI(), needContent);
  }

  private void removeDataManagedByFacet(final Connection con){
    Dataset needContent = rdfStorageService.loadDataset(con.getNeedURI());
    removeFacetManagedGraph(con.getNeedURI(), needContent);
    rdfStorageService.storeDataset(con.getNeedURI(), needContent);
  }

}
