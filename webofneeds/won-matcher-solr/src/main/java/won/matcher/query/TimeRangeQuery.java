package won.matcher.query;

import org.apache.lucene.search.BooleanClause;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 12.07.13
 * Time: 19:38
 * To change this template use File | Settings | File Templates.
 */
public class TimeRangeQuery extends LongRangeQuery
{
  public TimeRangeQuery(BooleanClause.Occur occur, String lowerBoundField, String upperBoundField)
  {
    super(occur, lowerBoundField, upperBoundField);
  }
}
