package won.protocol.util;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.eval.PathEval;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.util.ResourceUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.vocabulary.WON;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.*;

/**
 * Utilities for RDF manipulation with Jena.
 */
public class RdfUtils
{
  private static final Logger logger = LoggerFactory.getLogger(RdfUtils.class);

  public static String toString(Model model)
  {
    String ret = "";

    if (model != null) {
      StringWriter sw = new StringWriter();
      model.write(sw, "TTL");
      ret = sw.toString();
    }

    return ret;
  }

  public static Model toModel(String content)
  {
    return readRdfSnippet(content, FileUtils.langTurtle);
  }

  /**
   * Clones the specified model (its statements and ns prefixes) and returns the clone.
   * @param original
   * @return
   */
  public static Model cloneModel(Model original){
    Model clonedModel = ModelFactory.createDefaultModel();
    original.enterCriticalSection(Lock.READ);
    try {
      StmtIterator it = original.listStatements();
      while (it.hasNext()){
        clonedModel.add(it.nextStatement());
      }
      clonedModel.setNsPrefixes(original.getNsPrefixMap());
    } finally {
      original.leaveCriticalSection();
    }
    return clonedModel;
  }

  public static void replaceBaseURI(final Model model, final String baseURI)
  {
    //we assume that the RDF content is self-referential, i.e., it 'talks about itself': the graph is connected to
    //the public resource URI which, when de-referenced, returns that graph. So, triples referring to the 'null relative URI'
    //(see http://www.w3.org/2012/ldp/track/issues/20 ) will be changed to refer to the newly created need URI instead.
    //this implies that the default URI prefix of the document (if set) will have to be changed to the need URI.

    //check if there is a default URI prefix.
    //- If not, we just change the default prefix and that should automatically alter all
    //  null relative uris to refer to the newly set prefix.
    //- If there is one, fetch it as a resource and 'rename' it (i.e., replace all statements with exchanged name)
    if (model.getNsPrefixURI("") != null) {
      ResourceUtils.renameResource(
          model.getResource(model.getNsPrefixURI("")), baseURI
      );
    }
    //whatever the base uri (default URI prefix) was, set it to the need URI.
    model.setNsPrefix("", baseURI);
  }

  /**
   * Replaces the base URI that's set as the model's default URI prfefix in all statements by replacement.
   *
   * @param model
   * @param replacement
   */
  public static void replaceBaseResource(final Model model, final Resource replacement)
  {
    String baseURI = model.getNsPrefixURI("");
    if (baseURI == null) return;
    Resource baseUriResource = model.getResource(baseURI);
    replaceResourceInModel(baseUriResource, replacement);
    model.setNsPrefix("", replacement.getURI());
  }

  /**
   * Modifies the specified resources' model, replacing resource with replacement.
   * @param resource
   * @param replacement
   */
  private static void replaceResourceInModel(final Resource resource, final Resource replacement)
  {
    logger.debug("replacing resource '{}' with resource '{}'", resource, replacement);
    if (!resource.getModel().equals(replacement.getModel())) throw new IllegalArgumentException("resource and replacement must be from the same model");
    Model model = resource.getModel();
    Model modelForNewStatements = ModelFactory.createDefaultModel();
    StmtIterator iterator = model.listStatements(resource, (Property) null, (RDFNode) null);
    while (iterator.hasNext()) {
      Statement origStmt = iterator.next();
      Statement newStmt = new StatementImpl(replacement, origStmt.getPredicate(), origStmt.getObject());
      iterator.remove();
      modelForNewStatements.add(newStmt);
    }
    iterator = model.listStatements(null, (Property) null, (RDFNode) resource);
    while (iterator.hasNext()) {
      Statement origStmt = iterator.next();
      Statement newStmt = new StatementImpl(origStmt.getSubject(), origStmt.getPredicate(), replacement);
      iterator.remove();
      modelForNewStatements.add(newStmt);
    }
    model.add(modelForNewStatements);
  }

  /**
   * Creates a new model that contains both specified models' content. The base resource is that of model1,
   * all triples in model2 that are attached to the its base resource are modified so as to be attached to the
   * base resource of the result.
   * @param model1
   * @param model2
   * @return
   */
  public static Model mergeModelsCombiningBaseResource(final Model model1, final Model model2){
    if (logger.isDebugEnabled()){
      logger.debug("model1:\n{}",writeModelToString(model1, Lang.TTL));
      logger.debug("model2:\n{}",writeModelToString(model2, Lang.TTL));
    }
    Model result = ModelFactory.createDefaultModel();
    result.setNsPrefixes(mergeNsPrefixes(model1.getNsPrefixMap(), model2.getNsPrefixMap()));
    result.add(model1);
    result.add(model2);
    if (logger.isDebugEnabled()){
      logger.debug("result (before merging base resources):\n{}",writeModelToString(result, Lang.TTL));
    }
    Resource baseResource1 = getBaseResource(model1);
    Resource baseResource2 = getBaseResource(model2);
    replaceResourceInModel(result.getResource(baseResource1.getURI()), result.getResource(baseResource2.getURI()));
    result.setNsPrefix("",model1.getNsPrefixURI(""));
    if (logger.isDebugEnabled()){
      logger.debug("result (after merging base resources):\n{}",writeModelToString(result, Lang.TTL));
    }
    return result;
  }

  /**
   * Finds the resource representing the model's base resource, i.e. the resource with the
   * model's base URI. If no such URI is specified, a dummy base URI is set and a resource is
   * returned referencing that URI.
   *
   * @param model
   * @return
   */
  public static Resource findOrCreateBaseResource(Model model) {
    String baseURI = model.getNsPrefixURI("");
    if (baseURI == null){
      model.setNsPrefix("","no:uri");
      baseURI = model.getNsPrefixURI("");
    }
    return model.getResource(baseURI);
  }

  /**
   * Returns the resource representing the model's base resource, i.e. the resource with the
   * model's base URI.
   * @param model
   * @return
   */
  public static Resource getBaseResource(Model model){
    String baseURI = model.getNsPrefixURI("");
    if (baseURI == null) {
      return model.getResource("");
    } else {
      return model.getResource(baseURI);
    }
  }

  public static String writeModelToString(final Model model, final Lang lang)
  {
    StringWriter out = new StringWriter();
    RDFDataMgr.write(out, model, lang);
    return out.toString();
  }

  /**
   * Returns a copy of the specified resources' model where resource is replaced by replacement.
   * @param resource
   * @param replacement
   * @return
   */
  public static Model replaceResource(Resource resource, Resource replacement){
    if (!resource.getModel().equals(replacement.getModel())) throw new IllegalArgumentException("resource and replacement must be from the same model");
    Model result = ModelFactory.createDefaultModel();
    result.setNsPrefixes(resource.getModel().getNsPrefixMap());
    StmtIterator it = resource.getModel().listStatements();
    while (it.hasNext()){
      Statement stmt = it.nextStatement();
      Resource subject = stmt.getSubject();
      Resource predicate = stmt.getPredicate();
      RDFNode object = stmt.getObject();

      if (subject.equals(resource)){
        subject = replacement;
      }
      if (predicate.equals(resource)){
        predicate = replacement;
      }
      if (object.equals(resource)){
        object = replacement;
      }
      Triple triple = new Triple(subject.asNode(), predicate.asNode(), object.asNode());
      result.getGraph().add(triple);
    }
    return result;
  }

  /**
   * Adds the specified objectModel to the model of the specified subject. In the objectModel, the resource
   * that is identified by the objectModel's base URI (the "" URI prefix) will be replaced by a newly created
   * blank node(B, see later). All content of the objectModel is added to the model of the subject. An
   * additional triple (subject, property, B) is added. Moreover, the Namespace prefixes are merged.
   * @param subject
   * @param property
   * @param objectModel caution - will be modified
   */
  public static void attachModelByBaseResource(final Resource subject, final Property property, final Model objectModel)
  {
    attachModelByBaseResource(subject, property, objectModel, true);
  }

  /**
   * Adds the specified objectModel to the model of the specified subject. In the objectModel, the resource
   * that is identified by the objectModel's base URI (the "" URI prefix) will be replaced by a newly created
   * blank node(B, see later). All content of the objectModel is added to the model of the subject. An
   * additional triple (subject, property, B) is added. Moreover, the Namespace prefixes are merged.
   * @param subject
   * @param property
   * @param objectModel caution - will be modified
   * @param replaceBaseResourceByBlankNode
   */
  public static void attachModelByBaseResource(final Resource subject, final Property property, final Model objectModel, final boolean replaceBaseResourceByBlankNode){
    Model subjectModel = subject.getModel();
    //either explicitly use blank node, or do so if there is no base resource prefix
    //as the model may have triples containing the null relative URI.
    // we still want to attach these and try to get them by using
    //the empty URI, and replacing that resource by a blank node
    if (replaceBaseResourceByBlankNode || objectModel.getNsPrefixURI("") == null){
      //create temporary resource and replace objectModel's base resource to avoid clashes
      String tempURI = "tmp:"+Integer.toHexString(objectModel.hashCode());
      replaceBaseResource(objectModel, objectModel.createResource(tempURI));
      //merge models
      subjectModel.add(objectModel);
      //replace temporary resource by blank node
      Resource blankNode = subjectModel.createResource();
      subject.addProperty(property, blankNode);
      replaceResourceInModel(subjectModel.getResource(tempURI), blankNode);
      //merge the prefixes, but don't add the objectModel's default prefix in any case - we don't want it to end up as
      //the resulting model's base prefix.
      Map<String, String> objectModelPrefixes = objectModel.getNsPrefixMap();
      objectModelPrefixes.remove("");
      subjectModel.setNsPrefixes(mergeNsPrefixes(subjectModel.getNsPrefixMap(), objectModelPrefixes));
    } else {
      String baseURI = objectModel.getNsPrefixURI("");
      Resource baseResource = objectModel.getResource(baseURI); //getResource because it may exist already
      subjectModel.add(objectModel);
      baseResource = subjectModel.getResource(baseResource.getURI());
      subject.addProperty(property, baseResource);
      RdfUtils.replaceBaseResource(subjectModel, baseResource);
    }
  }

  /**
   * Creates a new Map object containing all prefixes from both specified maps. When prefix mappings clash, the mappings
   * from prioritaryPrefixes are used.
   * @param prioritaryPrefixes
   * @param additionalPrefixes
   * @return
   */
  public static Map<String, String> mergeNsPrefixes(final Map<String, String> prioritaryPrefixes, final Map<String, String> additionalPrefixes)
  {
    Map<String, String> mergedPrefixes = new HashMap<String, String>();
    mergedPrefixes.putAll(additionalPrefixes);
    mergedPrefixes.putAll(prioritaryPrefixes); //overwrites the additional prefixes when clashing
    return mergedPrefixes;
  }

  /**
   * Reads the InputStream into a Model. Sets a 'fantasy URI' as base URI and handles it gracefully if
   * the model read from the string defines its own base URI. Special care is taken that the null relative URI ('<>')
   * is replaced by the baseURI.
   * @param in
   * @param rdfLanguage
   * @return a Model (possibly empty)
   */
  public static Model readRdfSnippet(final InputStream in, final String rdfLanguage)
  {
    com.hp.hpl.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
    if (in == null) return model;
    String baseURI= "no:uri";
    model.setNsPrefix("", baseURI);
    model.read(in, baseURI, rdfLanguage);
    String baseURIAfterReading = model.getNsPrefixURI("");
    if (baseURIAfterReading == null){
      model.setNsPrefix("",baseURI);
    } else if (!baseURI.equals(baseURIAfterReading)){
      //the string representation of the model specified a base URI, but we used a different one for reading.
      //We need to make sure that the one that is now set is the only one used
      ResourceUtils.renameResource(model.getResource(baseURI), model.getNsPrefixURI(""));
    }
    return model;
  }

  /**
   * Reads the InputStream into a Model. Sets a 'fantasy URI' as base URI and handles it gracefully if
   * the model read from the string defines its own base URI. Special care is taken that the null relative URI ('<>')
   * is replaced by the baseURI.
   * @param in
   * @param rdfLanguage
   * @return a Model (possibly empty)
   */
  public static Model readRdfSnippet(final Reader in, final String rdfLanguage)
  {
    com.hp.hpl.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
    if (in == null) return model;
    String baseURI= "no:uri";
    model.setNsPrefix("", baseURI);
    model.read(in, baseURI, rdfLanguage);
    String baseURIAfterReading = model.getNsPrefixURI("");
    if (baseURIAfterReading == null){
      model.setNsPrefix("",baseURI);
    } else if (!baseURI.equals(baseURIAfterReading)){
      //the string representation of the model specified a base URI, but we used a different one for reading.
      //We need to make sure that the one that is now set is the only one used
      ResourceUtils.renameResource(model.getResource(baseURI), model.getNsPrefixURI(""));
    }
    return model;
  }

  /**
   * Reads the specified string into a Model. Sets a 'fantasy URI' as base URI and handles it gracefully if
   * the model read from the string defines its own base URI. Special care is taken that the null relative URI ('<>')
   * is replaced by the baseURI.
   * @param rdfAsString
   * @param rdfLanguage
   * @return a Model (possibly empty)
   */
  public static Model readRdfSnippet(final String rdfAsString, final String rdfLanguage)
  {
    return readRdfSnippet(new StringReader(rdfAsString), rdfLanguage);
  }

  /**
   * Looks for the triple [resourceURI, property, X] in the model obtained by
   * dereferencing the specified resourceURI and returns X as a URI.
   * If multiple triples are found, only the object of the first one is returned.
   * @param resourceURI
   * @param property
   * @return null if the model is empty or the property does not exist
   * @throws  IllegalArgumentException if the node found by the path is not a URI
   */
  public static URI getURIPropertyForResource(final Model model, final URI resourceURI, Property property)
  {
    StmtIterator stmts = model.listStatements(
        new SimpleSelector(model.createResource(resourceURI.toString()), property, (RDFNode) null));
    //assume only one endpoint
    if (!stmts.hasNext()) return null;
    Statement stmt = stmts.next();
    return URI.create(stmt.getObject().toString());
  }

  /**
   * Looks for the triple [resourceURI, property, X] in the model obtained by
   * dereferencing the specified resourceURI and returns X as a string.
   * If multiple triples are found, only the object of the first one is returned.
   * @param resourceURI
   * @param property
   * @return null if the model is empty or the property does not exist
   */
  public static String getStringPropertyForResource(final Model model, final URI resourceURI, Property property)
  {
    StmtIterator stmts = model.listStatements(
        new SimpleSelector(model.createResource(resourceURI.toString()), property, (RDFNode) null));
    //assume only one endpoint
    if (!stmts.hasNext()) return null;
    Statement stmt = stmts.next();
    return stmt.getString();
  }

  /**
   * Evaluates the path on the model obtained by dereferencing the specified resourceURI.
   * If the path resolves to multiple resources, only the first one is returned.
   * <br />
   * <br />
   * Note: For more information on property paths, see http://jena.sourceforge.net/ARQ/property_paths.html
   * <br />
   * To create a Path object for the path "rdf:type/rdfs:subClassOf*":
   * <pre>
   * Path path = PathParser.parse("rdf:type/rdfs:subClassOf*", PrefixMapping.Standard) ;
   * </pre>
   * @param resourceURI
   * @param propertyPath
   * @return null if the model is empty or the path does not resolve to a node
   * @throws  IllegalArgumentException if the node found by the path is not a URI
   */
  public static URI getURIPropertyForPropertyPath(final Model model, final URI resourceURI, Path propertyPath)
  {
    return toURI(getNodeForPropertyPath(model, resourceURI, propertyPath));
  }

  public static List<URI> getURIListForPropertyPath(final Model model, final URI resourceURI, Path propertyPath)
  {
    Iterator<Node> nodeIterator = getNodesForPropertyPath(model,resourceURI,propertyPath);
    List<URI> nodeURIList = new ArrayList<>();
    if (nodeIterator==null) return nodeURIList;
    if (!nodeIterator.hasNext()) return null;

    while(nodeIterator.hasNext()){
      nodeURIList.add(toURI(nodeIterator.next()));
    }
    return nodeURIList;
  }


  /**
   * Evaluates the path on the model obtained by dereferencing the specified resourceURI.
   * If the path resolves to multiple resources, only the first one is returned.
   * <br />
   * <br />
   * Note: For more information on property paths, see http://jena.sourceforge.net/ARQ/property_paths.html
   * <br />
   * To create a Path object for the path "rdf:type/rdfs:subClassOf*":
   * <pre>
   * Path path = PathParser.parse("rdf:type/rdfs:subClassOf*", PrefixMapping.Standard) ;
   * </pre>
   * @param resourceURI
   * @param propertyPath
   * @return null if the model is empty or the path does not resolve to a node
   */
  public static String getStringPropertyForPropertyPath(final Model model, final URI resourceURI, Path propertyPath)
  {
    return toString(getNodeForPropertyPath(model, resourceURI, propertyPath));
  }

  /**
   * Returns the literal lexical form of the specified node or null if the node is null.
   * @param node
   * @return
   */
  public static String toString(Node node){
    if (node == null) return null;
    return node.getLiteralLexicalForm();
  }

  /**
   * Returns the URI of the specified node or null if the node is null. If the node does not
   * represent a resource, an UnsupportedOperationException is thrown.
   * @param node
   * @return
   */
  public static URI toURI(Node node){
    if (node == null) return null;
    return URI.create(node.getURI());
  }


  /**
   * Returns the first RDF node found in the specified model for the specified property path.
   * @param model
   * @param resourceURI
   * @param propertyPath
   * @return
   */
  public static Node getNodeForPropertyPath(final Model model, URI resourceURI, Path propertyPath) {
    //Iterator<Node> result =  PathEval.eval(model.getGraph(), model.getResource(resourceURI.toString()).asNode(),
    //                                        propertyPath);
    Iterator<Node> result =  PathEval.eval(model.getGraph(), model.getResource(resourceURI.toString()).asNode(),
                                           propertyPath, Context.emptyContext);

    if (!result.hasNext()) return null;
    return result.next();
  }

  public static Iterator<Node> getNodesForPropertyPath(final Model model, URI resourceURI, Path propertyPath) {
    Iterator<Node> result =  PathEval.eval(model.getGraph(), model.getResource(resourceURI.toString()).asNode(), propertyPath);
    logger.debug("running path eval: "+ RdfUtils.toString(model));
    if (!result.hasNext()) return null;
    return result;
  }




  /**
   * Stores additional data if there is any in the specified model.
   * TODO: Move to WonRdfUtils
   *
   * @param eventURI
   * @param content
   * @param con
   * @param event
   * @param score
   */
  public static Model createContentForEvent(final URI eventURI, final Model content, final Connection con,
                                            final ConnectionEvent event, final Double score) {
    //TODO: define what content may contain and check that here! May content contain any RDF or must it be linked to the <> node?
    Model extraDataModel = ModelFactory.createDefaultModel();
    Resource eventNode = extraDataModel.createResource(eventURI.toString());
    if(score != null)
      eventNode.addLiteral(WON.HAS_MATCH_SCORE, score.doubleValue());
    extraDataModel.setNsPrefix("", eventNode.getURI().toString());
    if (content != null) {

      //TODO: check if the correct data is saved
      extraDataModel.add(content);
      RdfUtils.replaceBaseResource(extraDataModel, eventNode);
    }
    return extraDataModel;
  }
}
