package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;

/**
 * User: atus
 * Date: 28.06.13
 */
public abstract class AbstractExtendedQuery
{

  protected Query query;
  protected BooleanClause.Occur occur;

  public Query getQuery()
  {
    return query;
  }

  public BooleanClause.Occur getOccur()
  {
    return occur;
  }

}
