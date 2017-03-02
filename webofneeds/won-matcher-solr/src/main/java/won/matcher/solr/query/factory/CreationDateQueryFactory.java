package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import won.protocol.util.WonRdfUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

/**
 * Created by hfriedrich on 18.08.2016.
 */
public class CreationDateQueryFactory extends NeedDatasetQueryFactory
{
  private static String CREATION_DATE_SOLR_FIELD = "_graph.http___purl.org_dc_terms_created._value";

  private ZonedDateTime startDate;
  private ZonedDateTime endDate;

  public CreationDateQueryFactory(Dataset needDataset, long timeWindow, TemporalUnit unit) {
    super(needDataset);

    String needUri = WonRdfUtils.NeedUtils.getNeedURI(needDataset).toString();
    Model model = WonRdfUtils.NeedUtils.getNeedModelFromNeedDataset(needDataset);
    Resource need = model.getResource(needUri.toString());

    Statement creationDateStat = need.getProperty(DCTerms.created);
    if (creationDateStat == null) {
      throw new IllegalArgumentException("expected field creation date in need " + needUri);
    }

    ZonedDateTime creationDate = ZonedDateTime.parse(creationDateStat.getString(), DateTimeFormatter.ISO_DATE_TIME);
    startDate = creationDate.plus(timeWindow, ChronoUnit.MINUTES);
    startDate = creationDate.minus(timeWindow, unit);
    endDate = creationDate.plus(timeWindow, unit);
  }

  @Override
  protected String makeQueryString() {
    return new DateIntervalQueryFactory(CREATION_DATE_SOLR_FIELD, startDate, endDate).createQuery();
  }
}
