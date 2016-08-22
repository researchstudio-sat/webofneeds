package won.matcher.solr.query.factory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import won.matcher.solr.index.NeedIndexer;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Created by hfriedrich on 19.08.2016.
 */
public class LocationParamsFactory implements SolrParamsFactory
{
  ModifiableSolrParams params = null;

  public LocationParamsFactory(Dataset needDataset) {

    // extract the location fields from the need
    params = new ModifiableSolrParams();
    Model needModel = WonRdfUtils.NeedUtils.getNeedModelFromNeedDataset(needDataset);
    URI needUri = WonRdfUtils.NeedUtils.getNeedURI(needModel);
    Float longitude = WonRdfUtils.NeedUtils.getLocationLongitude(needModel, needUri);
    Float latitude = WonRdfUtils.NeedUtils.getLocationLatitude(needModel, needUri);
    if (latitude != null && longitude != null) {
      params.add("sfield", NeedIndexer.SOLR_LOCATION_COORDINATES_FIELD);
      params.add("pt", longitude + "," + latitude);
    }
  }

  @Override
  public SolrParams createParams() {
    return params;
  }
}
