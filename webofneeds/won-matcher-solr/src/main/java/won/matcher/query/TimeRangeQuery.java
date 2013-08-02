package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.schema.TrieDateField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;

/**
 * User: atus
 * Date: 18.07.13
 * Time: 14:38
 */
public class TimeRangeQuery extends LongRangeQuery
{
  private Logger logger = LoggerFactory.getLogger(getClass());

  public TimeRangeQuery(BooleanClause.Occur occur, String lowerBoundField, String upperBoundField)
  {
    super(occur, lowerBoundField, upperBoundField);
  }

  @Override
  protected Long getField(final SolrInputDocument inputDocument, final String fieldName)
  {
    if (!inputDocument.containsKey(fieldName))
      return null;

    String data = inputDocument.getFieldValue(fieldName).toString();
    try {
      Date date = TrieDateField.parseDate(data);
      logger.info("Date field output: " + date.toString());
      return date.getTime();
    } catch (ParseException e) {
      logger.error("Error parsing date", e);
      return null;
    }
  }
}
