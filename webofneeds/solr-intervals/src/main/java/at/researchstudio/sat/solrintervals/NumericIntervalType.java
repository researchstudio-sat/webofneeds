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

import java.io.IOException;
import java.util.Map;

/**
 * An abstract numeric interval SOLR field type that allows you to store numeric intervals and query them.
 * Numeric intervals are stored as <code>[start]-[stop]</code>. That is two numbers with
 * a minus (dash) used as a separator.
 *
 * User: atus
 * Date: 15.10.13
 */
public abstract class NumericIntervalType<T extends Number> extends AbstractSubTypeFieldType
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
      String[] startStop = splitExternal(toInternal(externalVal));

      int i = 0;
      SchemaField sf = subField(field, i);
      f[i] = sf.createField(startStop[START], boost);
      i++;
      f[i] = subField(field, i).createField(startStop[STOP], boost);
    }

    if (field.stored()) {
      f[f.length - 1] = createField(field.getName(),
          toInternal(externalVal),
          getFieldStore(field, toInternal(externalVal)),
          Field.Index.NO,
          Field.TermVector.NO,
          false,
          FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS,
          boost);
    }

    return f;
  }

  //One does not simply create a single field
  @Override
  public Fieldable createField(SchemaField field, String externalVal, float boost)
  {
    throw new UnsupportedOperationException("NumericIntervalType uses multiple fields.  field=" + field.getName());
  }

  @Override
  public Query getFieldQuery(QParser parser, SchemaField field, String externalVal)
  {
    int dimension = 2;

    String[] startStop = splitExternal(externalVal);

    BooleanQuery bq = new BooleanQuery(true);
    for (int i = 0; i < dimension; i++) {
      SchemaField sf = subField(field, i);
      Query tq = sf.getType().getFieldQuery(parser, sf, startStop[i]);
      bq.add(tq, BooleanClause.Occur.MUST);
    }

    return bq;
  }

  @Override
  public Query getRangeQuery(QParser parser, SchemaField field, String part1, String part2, boolean minInclusive, boolean maxInclusive)
  {
    int dimension = 2;

    //TODO research this true in the constructor
    BooleanQuery query = new BooleanQuery(true);
    for (int i = 0; i < dimension; i++) {
      SchemaField subSF = subField(field, i);
      // points must be ordered
      query.add(subSF.getType().getRangeQuery(parser, subSF, subFieldToInternal(part1), subFieldToInternal(part2), minInclusive, maxInclusive),
          BooleanClause.Occur.SHOULD);
    }

    return query;
  }

  /**
   * Override this function if you have to convert the subfields to a correct representation.
   *
   * @param external external representation
   * @return internal representation
   */
  protected String subFieldToInternal(String external)
  {
    return external;
  }

  /**
   * Splits the external representation to two strings and validates that the parts are readable.
   *
   * @param external string to be split
   * @return an array with the start and stop value <code>long[]{start,stop}</code>
   * @throws SolrException if the external value has the wrong formatting
   */
  protected String[] splitExternal(String external)
  {
    String[] parts = external.split(SEPARATOR);
    if (parts.length != 2)
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "NumericIntervalType has the following form: <start>-<stop>");

    validateExternal(parts);

    return parts;
  }

  /**
   * Validates that the given strings can be parsed to the right number type.
   *
   * @param parts start and stop string representations
   * @return true if both parts are parseable, false otherwise
   */
  public abstract boolean validateExternal(String[] parts);

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

  @Override
  public SortField getSortField(final SchemaField field, final boolean top)
  {
    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Sorting not supported on NumericIntervalType " + field.getName());
  }

  @Override
  public boolean isPolyField()
  {
    return true;
  }


}
