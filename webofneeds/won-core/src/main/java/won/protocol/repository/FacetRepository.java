package won.protocol.repository;

import java.net.URI;
import java.util.List;

import won.protocol.model.Facet;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 10.09.13 Time: 17:14 To
 * change this template use File | Settings | File Templates.
 */
public interface FacetRepository extends WonRepository<Facet> {
  List<Facet> findByNeedURI(URI needURI);

  List<Facet> findByNeedURIAndTypeURI(URI needURI, URI typeURI);

  List<Facet> findByNeedURIAndFacetURI(URI needURI, URI facetURI);

  Facet findOneByNeedURIAndTypeURI(URI needURI, URI typeURI);

  Facet findOneByNeedURIAndFacetURI(URI needURI, URI facetURI);

  Facet findOneByFacetURI(URI facetURI);
}
