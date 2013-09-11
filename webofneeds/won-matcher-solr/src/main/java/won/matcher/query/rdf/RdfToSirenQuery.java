/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.matcher.query.rdf;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Literal;
import com.hp.hpl.jena.graph.Triple;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.util.Version;
import org.apache.solr.schema.IndexSchema;
import org.sindice.siren.analysis.NumericAnalyzer;
import org.sindice.siren.qparser.tuple.ResourceQueryParser;
import org.sindice.siren.search.*;
import org.sindice.siren.solr.schema.Datatype;
import org.sindice.siren.solr.schema.SirenField;
import org.sindice.siren.util.XSDDatatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.query.rdf.expr.QueriesForVariables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RdfToSirenQuery
{
  private static final Pattern PATTERN_REMOVE_QUOTES = Pattern.compile("\"(.+)\".*");
  private static final BooleanClause.Occur DEFAULT_OCCUR = BooleanClause.Occur.SHOULD;
  private static final SirenBooleanClause.Occur OCCUR_LITERAL = SirenBooleanClause.Occur.MUST;
  private static final SirenTupleClause.Occur OCCUR_TRIPLE = SirenTupleClause.Occur.MUST;
  private static final BooleanClause.Occur OCCUR_STAR = BooleanClause.Occur.MUST;
  private static final BooleanClause.Occur OCCUR_ALL = BooleanClause.Occur.MUST;

  private static Logger logger = LoggerFactory.getLogger(RdfToSirenQuery.class);

  private IndexSchema indexSchema;
  private WhitespaceAnalyzer wsAnalyzer = new WhitespaceAnalyzer(Version.LUCENE_35);
  private DefaultOperatorAttribute.Operator defaultOp = DefaultOperatorAttribute.Operator.AND;
  private final Map<String, Analyzer> datatypeConfig = new HashMap<String, Analyzer>();
  private String field;

  public RdfToSirenQuery(final IndexSchema indexSchema, final String field)
  {
    this.indexSchema = indexSchema;
    this.field = field;
    loadDatatypeConfig();
  }



  public BooleanQuery createQueryForTriples(List<Triple> triples, QueriesForVariables queriesForVariables, String field)
  {
    if (queriesForVariables == null) queriesForVariables = new QueriesForVariables();
    BooleanQuery query = new BooleanQuery();
    for (Triple triple : triples) {
      SirenTupleQuery tupleQuery = new SirenTupleQuery();
      fillTupleQueryFromTriple(triple, tupleQuery, queriesForVariables, field);
      if (tupleQuery.getClauses().length > 0) {
        query.add(tupleQuery, OCCUR_STAR);
      }
    }
    return query;
  }

  private void fillTupleQueryFromTriple(final Triple triple, final SirenTupleQuery sirenTupleQuery, final QueriesForVariables queriesForVariables, String field)
  {
    //subject
    addQueryIfNotNull(sirenTupleQuery, createSirenBooleanQueryForNode(triple.getSubject(), queriesForVariables, field), 0);
    //predicate
    addQueryIfNotNull(sirenTupleQuery, createSirenBooleanQueryForNode(triple.getPredicate(), queriesForVariables, field), 1);
    //object
    SirenBooleanQuery queryForObject = createSirenBooleanQueryForNode(triple.getObject(), queriesForVariables, field);
    if (queryForObject != null && queryForObject.getBoost() == 1f) {
      queryForObject.setBoost(1f);
    }
    addQueryIfNotNull(sirenTupleQuery, queryForObject, 2);
  }

  private void addQueryIfNotNull(SirenTupleQuery sirenTupleQuery, SirenBooleanQuery sirenBooleanQueryToAdd, int constraint)
  {
    if (sirenBooleanQueryToAdd == null) return;
    SirenCellQuery cellQuery = new SirenCellQuery(sirenBooleanQueryToAdd);
    cellQuery.setConstraint(constraint);
    sirenTupleQuery.add(cellQuery, OCCUR_TRIPLE);
  }

  /**
   * Returns a SirenBooleanQuery that will match the specified node or null
   * if there should not be a restriction.
   *
   * @param node
   * @param queriesForVariables
   */
  public SirenBooleanQuery createSirenBooleanQueryForNode(Node node, final QueriesForVariables queriesForVariables, String field)
  {
    SirenBooleanQuery query = new SirenBooleanQuery();
    SirenPrimitiveQuery subQuery = createSirenPrimitiveQueryForNode(node, queriesForVariables, field);
    if (subQuery == null) return null;
    query.add(subQuery, OCCUR_LITERAL);
    return query;
  }

  public SirenPrimitiveQuery createSirenPrimitiveQueryForNode(Node node, QueriesForVariables queriesForVariables, String field){
    if (node.isBlank()) return null;
    if (node.isVariable()) {
      SirenBooleanQuery queryForVar = queriesForVariables.getQueryForVariable(node.getName());
      logger.debug("query for variable {}: {}", node.getName(), queryForVar);
      //TODO: make boost and occur configurable!
      queryForVar.setBoost(1f);
      return queryForVar;
    }

    String nodeAsString = toQuotedString(node);
    Analyzer analyzer = getAnalyzerForNode(node);
    logger.debug("using analyzer {} for node value {}", analyzer, nodeAsString);
    ResourceQueryParser parser = getResourceQueryParser(analyzer);
    SirenPrimitiveQuery query = null;
    try {
      //we have to put the nodeAsString value in quotes so the lucene query parser doesnt't find lucene syntax
      query = (SirenPrimitiveQuery) parser.parse("\""+nodeAsString+"\"", field);
    } catch (QueryNodeException e) {
      logger.debug("could not parse node value '{}' as SIREn query ", nodeAsString, e);
    }
    logger.debug("query for node: " + query);
    return query;
  }

  /**
   * adds <..> around an URI.
   *
   * @param node
   * @return
   */
  private String toQuotedString(Node node)
  {
    if (node.isURI()) return node.toString().toLowerCase();
    if (node.isLiteral()) {
      String nodeAsString = node.toString(true).toLowerCase();
      Matcher matcher = PATTERN_REMOVE_QUOTES.matcher(nodeAsString);
      if (matcher.matches()) {
        return matcher.group(1);
      }
      return nodeAsString;
    }
    return node.toString();
  }


  /**
   * Instantiate a {@link org.sindice.siren.qparser.tuple.ResourceQueryParser} depending on the object type.
   * Then, set the default operator.
   */
  protected ResourceQueryParser getResourceQueryParser(final Analyzer analyzer) {
    final ResourceQueryParser qph;

    if (analyzer instanceof NumericAnalyzer)
      qph = new ResourceQueryParser(wsAnalyzer, (NumericAnalyzer) analyzer);
    else
      qph = new ResourceQueryParser(analyzer);
    qph.setDefaultOperator(defaultOp);
    return qph;
  }

  /**
   * Gets the right analyzer based on node type. Returns the field's standard
   * query analyzer as a fallback.
   * In a standard setup, analyzers are configured in
   * the solr config file tuple-datatypes.xml and tuple-analyzers.xml.
   * @param node
   * @return
   */
  private Analyzer getAnalyzerForNode(Node node){
    if (node.isLiteral()){
      String datatypeURI = getDatatypeURIForLiteral((Node_Literal) node);
      return getAnalyzerForDatatype(datatypeURI);
    } else if (node.isURI()){
      return getAnalyzerForDatatype(XSDDatatype.XSD_ANY_URI);
    }
    return this.indexSchema.getFieldType(field).getQueryAnalyzer();
  }

  /**
   * Gets the analyzer for the specified datatype. Returns the field's standard query analyzer as a fallback.
   * @param datatype
   * @return
   */
  private Analyzer getAnalyzerForDatatype(String datatype){
    Analyzer analyzer = this.datatypeConfig.get(datatype);
    if (analyzer != null) return analyzer;
    return this.indexSchema.getFieldType(field).getQueryAnalyzer();
  }

  /**
   * Fetches the datatype URI for the specified literal. If no datatype is specified, xsd:string is returned.
   * @param l
   * @return
   */
  private String getDatatypeURIForLiteral(Node_Literal l){
    String datatypeURI = l.getLiteralDatatypeURI();
    if (datatypeURI == null) return XSDDatatype.XSD_STRING;
    return datatypeURI;
  }

  /**
   * Loads the datatype configuration (Datatype-string to Analyzer mapping) for the field from the indexSchema.
   */
  private void loadDatatypeConfig()
  {
    Map<String, Datatype> datatypeMap = ((SirenField) this.indexSchema.getFieldType(field)).getDatatypes();
    for(Map.Entry<String, Datatype> entry: datatypeMap.entrySet()){
      this.datatypeConfig.put(entry.getKey(), entry.getValue().getQueryAnalyzer());
    }
  }

}