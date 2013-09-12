package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.spatial.geometry.DistanceUnits;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.LatLonType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SpatialFilterQParser;
import org.apache.solr.search.SpatialOptions;

import java.io.IOException;

/**
 * User: atus
 * Date: 05.07.13
 */
public class SpatialQueryFactory extends AbstractQueryFactory
{
  private final int MAX_DISTANCE = 10;

  private String locationField;

  public SpatialQueryFactory(BooleanClause.Occur occur, float boost, String locationField)
  {
    super(occur, boost);
    this.locationField = locationField;
  }

  public SpatialQueryFactory(final BooleanClause.Occur occur, final String locationField)
  {
    super(occur);
    this.locationField = locationField;
  }

  @Override
  public Query createQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    if (!inputDocument.containsKey(locationField))
      return null;

    FieldType latLon = indexSearcher.getSchema().getFieldTypeByName(locationField);

    SpatialOptions options = new SpatialOptions();
    options.radius = 6371;
    options.distance = MAX_DISTANCE;
    options.units = DistanceUnits.KILOMETERS;
    options.pointStr = inputDocument.getFieldValue(locationField).toString();
    options.field = new SchemaField(locationField, latLon);

    NamedList<String> solrParamsList = new NamedList();
    solrParamsList.add("sfield", locationField);
    solrParamsList.add("pt", inputDocument.getFieldValue(locationField).toString());
    solrParamsList.add("d", String.valueOf(MAX_DISTANCE));
    SolrParams solrParams = SolrParams.toSolrParams(solrParamsList);

    SolrCore solrCore = indexSearcher.getCore();
    SolrQueryRequest sqr = new LocalSolrQueryRequest(solrCore, solrParams);
    SpatialFilterQParser qParser = new SpatialFilterQParser("geofilt", null, solrParams, sqr, false);
    Query query = ((LatLonType) latLon).createSpatialQuery(qParser, options);

    return query;
  }
}
