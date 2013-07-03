package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 02.07.13
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
public class TimeRangeFilterQuery {
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_END_TIME = "endTime";

    private BooleanQuery query;

    public TimeRangeFilterQuery(long lower, long upper) {
       Query nq1 = NumericRangeQuery.newLongRange(FIELD_START_TIME, lower, upper, true, true);
       Query nq2 = NumericRangeQuery.newLongRange(FIELD_END_TIME, lower, upper, true, true);

       query = new BooleanQuery();

       //one of the two query must match at least one document
       query.add(nq1, BooleanClause.Occur.SHOULD);
       query.add(nq2, BooleanClause.Occur.SHOULD);
    }

    public Query getQuery() {
        return query;
    }
}
