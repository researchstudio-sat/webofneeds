package won.matcher.solr.query.factory;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import org.apache.jena.query.Dataset;

import won.protocol.util.AtomModelWrapper;

/**
 * Created by hfriedrich on 18.08.2016.
 */
public class CreationDateQueryFactory extends AtomDatasetQueryFactory {
    private static String CREATION_DATE_SOLR_FIELD = "_graph.http___purl.org_dc_terms_created._value";
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;

    public CreationDateQueryFactory(Dataset atomDataset, long timeWindow, TemporalUnit unit) {
        super(atomDataset);
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        ZonedDateTime creationDate = atomModelWrapper.getCreationDate();
        startDate = creationDate.plus(timeWindow, ChronoUnit.MINUTES);
        startDate = creationDate.minus(timeWindow, unit);
        endDate = creationDate.plus(timeWindow, unit);
    }

    @Override
    protected String makeQueryString() {
        return new DateIntervalQueryFactory(CREATION_DATE_SOLR_FIELD, startDate, endDate).createQuery();
    }
}
