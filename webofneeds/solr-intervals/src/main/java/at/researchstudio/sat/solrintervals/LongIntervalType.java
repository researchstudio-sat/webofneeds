package at.researchstudio.sat.solrintervals;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.solr.common.SolrException;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.response.XMLWriter;
import org.apache.solr.schema.AbstractSubTypeFieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QParser;
import org.apache.solr.search.function.ValueSource;

import java.io.IOException;
import java.util.Map;

/**
 * A long interval SOLR field type that allows you to store long intervals and query them.
 * Long intervals are stored as <code>[start]-[stop]</code>. That are two longs with
 * a minus (dash) used as a separator.
 *
 * User: atus
 * Date: 11.10.13
 */
public class LongIntervalType extends AbstractSubTypeFieldType
{
  public static final int START = 0;
  public static final int STOP = 1;
  public static final String SEPARATOR = "-";

  @Override
  protected void init(final IndexSchema schema, final Map<String, String> args)
  {
    super.init(schema, args);

    createSuffixCache(3);
  }

  @Override
  public Fieldable[] createFields(final SchemaField field, final String externalVal, final float boost)
  {
    Fieldable[] f = new Fieldable[(field.indexed() ? 2 : 0) + (field.stored() ? 1 : 0)];

    if (field.indexed()) {
      long[] startStop = parseExternal(externalVal);

      int i = 0;
      SchemaField sf = subField(field, i);
      f[i] = sf.createField(String.valueOf(startStop[START]), boost);
      i++;
      f[i] = subField(field, i).createField(String.valueOf(startStop[STOP]), boost);
    }

    if (field.stored()) {
      f[f.length - 1] = createField(field.getName(),
          externalVal,
          getFieldStore(field, externalVal),
          Field.Index.NO,
          Field.TermVector.NO,
          false,
          FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS,
          boost);
    }

    return f;
  }

  /**
   * Splits the external representation to two longs.
   *
   * @param external string to be parsed
   * @return an array with the start and stop value <code>long[]{start,stop}</code>
   */
  public static long[] parseExternal(String external)
  {
    String[] parts = external.split(SEPARATOR);
    if (parts.length != 2)
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "LongIntervalField has the following form: <start>-<stop>");

    long[] interval = new long[2];

    try {
      interval[0] = Long.parseLong(parts[0]);
      interval[1] = Long.parseLong(parts[1]);
    } catch (NumberFormatException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "LongIntervalField: error while parsing values: " + e.getMessage());
    }

    return interval;
  }

  @Override
  public Query getFieldQuery(QParser parser, SchemaField field, String externalVal)
  {
    int dimension = 2;

    long[] startStop = parseExternal(externalVal);

    BooleanQuery bq = new BooleanQuery(true);
    for (int i = 0; i < dimension; i++) {
      SchemaField sf = subField(field, i);
      Query tq = sf.getType().getFieldQuery(parser, sf, String.valueOf(startStop[i]));
      bq.add(tq, BooleanClause.Occur.MUST);
    }

    return bq;
  }

  @Override
  public Query getRangeQuery(QParser parser, SchemaField field, String part1, String part2, boolean minInclusive, boolean maxInclusive)
  {
    int dimension = 2;

    BooleanQuery result = new BooleanQuery(true);
    for (int i = 0; i < dimension; i++) {
      SchemaField subSF = subField(field, i);
      // points must be ordered
      result.add(subSF.getType().getRangeQuery(parser, subSF, part1, part2, minInclusive, maxInclusive),
          BooleanClause.Occur.SHOULD);
    }
    return result;
  }

  //TODO I know this is crazy, but implement this maybe
  @Override
  public ValueSource getValueSource(final SchemaField field, final QParser parser)
  {
    return super.getValueSource(field, parser);    //To change body of overridden methods use File | Settings | File Templates.
  }

  @Override
  public void write(final XMLWriter xmlWriter, final String name, final Fieldable f) throws IOException
  {
    xmlWriter.writeStr(name, f.stringValue());
  }

  @Override
  public void write(final TextResponseWriter writer, final String name, final Fieldable f) throws IOException
  {
    writer.writeStr(name, f.stringValue(), false);
  }

  //One does not simply create a single field
  @Override
  public Fieldable createField(SchemaField field, String externalVal, float boost)
  {
    throw new UnsupportedOperationException("LongIntervalField uses multiple fields.  field=" + field.getName());
  }

  @Override
  public SortField getSortField(final SchemaField field, final boolean top)
  {
    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Sorting not supported on LongIntervalField " + field.getName());
  }

  @Override
  public boolean isPolyField()
  {
    return true;
  }

}
