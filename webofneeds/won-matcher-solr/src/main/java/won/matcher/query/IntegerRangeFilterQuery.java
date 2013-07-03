package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 03.07.13
 * Time: 12:56
 * To change this template use File | Settings | File Templates.
 */
public class IntegerRangeFilterQuery {
    private static final String FIELD_LOWER_PRICE_LIMIT = "lowerPriceLimit";
    private static final String FIELD_UPPER_PRICE_LIMIT = "upperPriceLimit";

    private BooleanQuery query;

    public IntegerRangeFilterQuery(double lower, double upper) {
        Query nq1 = NumericRangeQuery.newDoubleRange(FIELD_LOWER_PRICE_LIMIT, lower, upper, true, true);
        Query nq2 = NumericRangeQuery.newDoubleRange(FIELD_UPPER_PRICE_LIMIT, lower, upper, true, true);

        query = new BooleanQuery();

        //one of the two query must match at least one document
        query.add(nq1, BooleanClause.Occur.SHOULD);
        query.add(nq2, BooleanClause.Occur.SHOULD);
    }

    public Query getQuery() {
        return query;
    }
}
