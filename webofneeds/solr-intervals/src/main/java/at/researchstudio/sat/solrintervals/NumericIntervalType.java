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
import org.apache.solr.search.function.VectorValueSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A numeric interval SOLR field type that allows you to store numeric intervals and query them.
 * Numeric intervals are stored as <code>[start][SEPARATOR][stop]</code>. That is two numbers with
 * a separator in between.
 * The separator can be set using SOLR arguments in the schema file.
 * <p/>
 * User: atus
 * Date: 15.10.13
 */
public class NumericIntervalType extends AbstractSubTypeFieldType
{
  protected final int START = 0;
  protected final int STOP = 1;

  public static final String SEPARATOR_ARG = "separator";
  protected String separator;

  @Override
  protected void init(final IndexSchema schema, final Map<String, String> args)
  {
    super.init(schema, args);
    createSuffixCache(3);

    if (args.containsKey(SEPARATOR_ARG))
      separator = args.get(SEPARATOR_ARG);
    else
      separator = getDefaultSeparator();
  }

  @Override
  public Fieldable[] createFields(final SchemaField field, final String externalVal, final float boost)
  {
    Fieldable[] f = new Fieldable[(field.indexed() ? 2 : 0) + (field.stored() ? 1 : 0)];

    if (field.indexed()) {
      String[] startStop = splitExternal(externalVal);

      int i = 0;
      SchemaField sf = subField(field, i);
      f[i] = sf.createField(startStop[START], boost);
      i++;
      f[i] = subField(field, i).createField(startStop[STOP], boost);
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

  //One does not simply create a single field
  @Override
  public Fieldable createField(SchemaField field, String externalVal, float boost)
  {
    throw new UnsupportedOperationException("NumericIntervalType uses multiple fields. field=" + field.getName());
  }

  @Override
  public Query getFieldQuery(QParser parser, SchemaField field, String externalVal)
  {
    int dimension = 2;

    String[] startStop = splitExternal(externalVal);

    BooleanQuery query = new BooleanQuery(true);
    for (int i = 0; i < dimension; i++) {
      SchemaField sf = subField(field, i);
      Query tq = sf.getType().getFieldQuery(parser, sf, startStop[i]);
      query.add(tq, BooleanClause.Occur.MUST);
    }

    return query;
  }

  @Override
  public Query getRangeQuery(QParser parser, SchemaField field, String part1, String part2, boolean leftInclusive, boolean rightInclusive)
  {
    //TODO research this true in the constructor

    SchemaField subField1 = subField(field, 0);
    SchemaField subField2 = subField(field, 1);

    // points must be ordered
    BooleanQuery mainQuery = new BooleanQuery(true);

    BooleanQuery internalRangeQuery = new BooleanQuery(true);
    internalRangeQuery.add(
        subField1.getType().getRangeQuery(parser, subField1, null, part1, leftInclusive, rightInclusive),
        BooleanClause.Occur.MUST);
    internalRangeQuery.add(
        subField2.getType().getRangeQuery(parser, subField2, part2, null, leftInclusive, rightInclusive),
        BooleanClause.Occur.MUST);
    mainQuery.add(internalRangeQuery, BooleanClause.Occur.SHOULD);

    mainQuery.add(
        subField1.getType().getRangeQuery(parser, subField1, part1, part2, leftInclusive, rightInclusive),
        BooleanClause.Occur.SHOULD);

    mainQuery.add(
        subField2.getType().getRangeQuery(parser, subField2, part1, part2, leftInclusive, rightInclusive),
        BooleanClause.Occur.SHOULD);

    return mainQuery;
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
    String[] parts = external.split(separator);
    if (parts.length != 2)
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "NumericIntervalType has the following form: <start>" + separator + "<stop>");

    return parts;
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

  @Override
  public SortField getSortField(final SchemaField field, final boolean top)
  {
    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Sorting not supported on NumericIntervalType " + field.getName());
  }

  @Override
  public ValueSource getValueSource(SchemaField field, QParser parser)
  {
    List<ValueSource> vs = new ArrayList<>(2);
    for (int i = 0; i < 2; i++) {
      SchemaField sub = subField(field, i);
      vs.add(sub.getType().getValueSource(sub, parser));
    }
    return new NumericIntervalValueSource(field, vs);
  }

  @Override
  public boolean isPolyField()
  {
    return true;
  }

  public String getDefaultSeparator()
  {
    return "-";
  }

  class NumericIntervalValueSource extends VectorValueSource
  {
    private final SchemaField sf;

    public NumericIntervalValueSource(SchemaField sf, List<ValueSource> sources)
    {
      super(sources);
      this.sf = sf;
    }

    @Override
    public String name()
    {
      return "numericInterval";
    }

    @Override
    public String description()
    {
      return name() + "(" + sf.getName() + ")";
    }
  }


}