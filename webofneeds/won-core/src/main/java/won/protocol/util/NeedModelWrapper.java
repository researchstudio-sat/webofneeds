package won.protocol.util;

import com.google.common.collect.HashBiMap;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import won.protocol.exception.DataIntegrityException;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.model.*;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by hfriedrich on 16.03.2017.
 */
public class NeedModelWrapper
{
  private Model needModel;
  private Model sysInfoModel;
  private final HashBiMap<MatchingBehaviorType, Resource> matchingBehaviorMap = initMap();

  public NeedModelWrapper(String needUri) {

    needModel = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(needModel);
    needModel.createResource(needUri, WON.NEED);
    sysInfoModel = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(sysInfoModel);
    sysInfoModel.createResource(needUri, WON.NEED);
  }

  public NeedModelWrapper(Dataset ds) {

    // find the need model
    Iterator<String> iter = ds.listNames();
    while (iter.hasNext()) {
      String m = iter.next();
      if (m.endsWith("#need")) {
        needModel = ds.getNamedModel(m);
      } else if (m.endsWith("#sysinfo")) {
        sysInfoModel = ds.getNamedModel(m);
      }
    }
    checkModels();
  }

    public NeedModelWrapper(Model needModel, Model sysInfoModel) {

      this.needModel = needModel;
      this.sysInfoModel = sysInfoModel;
      checkModels();
  }

  private HashBiMap initMap() {

    HashBiMap<MatchingBehaviorType, Resource> matchingBehaviorMap = HashBiMap.create();
    matchingBehaviorMap.put(MatchingBehaviorType.MUTUAL, WON.MATCHING_BEHAVIOR_MUTUAL);
    matchingBehaviorMap.put(MatchingBehaviorType.DO_NOT_MATCH, WON.MATCHING_BEHAVIOR_DO_NOT_MATCH);
    matchingBehaviorMap.put(MatchingBehaviorType.LAZY, WON.MATCHING_BEHAVIOR_LAZY);
    matchingBehaviorMap.put(MatchingBehaviorType.STEALTHY, WON.MATCHING_BEHAVIOR_STEALTHY);
    return matchingBehaviorMap;
  }

  public void checkModels() {
    try {
      getNeedNode(NeedGraphType.NEED);
      getNeedNode(NeedGraphType.SYSINFO);
    } catch (NullPointerException e1) {
      throw new DataIntegrityException("both need and sysinfo graphs must exist in dataset", e1);
    } catch (IncorrectPropertyCountException e2) {
      throw new DataIntegrityException("need and sysinfo models must be a won:Need");
    }
  }

  public Model getNeedModel(NeedGraphType graph) {

    if (graph.equals(NeedGraphType.NEED)) {
      return needModel;
    } else {
      return sysInfoModel;
    }
  }

  public Resource getNeedNode(NeedGraphType graph) {

    if (graph.equals(NeedGraphType.NEED)) {
      return RdfUtils.findOneSubjectResource(needModel, RDF.type, WON.NEED);
    } else {
      return RdfUtils.findOneSubjectResource(sysInfoModel, RDF.type, WON.NEED);
    }
  }

  public String getNeedUri() {
    return getNeedNode(NeedGraphType.NEED).getURI();
  }

  public Resource createContentNode(NeedContentPropertyType type, String uri) {

    if (NeedContentPropertyType.ALL.equals(type)) {
      throw new IllegalArgumentException("NeedContentPropertyType.ALL not defined for this method");
    }

    Resource contentNode = (uri != null) ? needModel.createResource(uri) : needModel.createResource();
    contentNode.addProperty(RDF.type, WON.NEED_CONTENT);
    Resource needNode = getNeedNode(NeedGraphType.NEED);

    if (NeedContentPropertyType.IS.equals(type)) {
      needNode.addProperty(WON.IS, contentNode);
    } else if (NeedContentPropertyType.SEEKS.equals(type)) {
      needNode.addProperty(WON.SEEKS, contentNode);
    } else if (NeedContentPropertyType.SEEKS_SEEKS.equals(type)) {
      Resource intermediate = needModel.createResource();
      needNode.addProperty(WON.SEEKS, intermediate);
      intermediate.addProperty(WON.SEEKS, contentNode);
    } else if (NeedContentPropertyType.IS_AND_SEEKS.equals(type)) {
      needNode.addProperty(WON.IS, contentNode);
      needNode.addProperty(WON.SEEKS, contentNode);
    }

    return contentNode;
  }

  public void setMatchingBehavior(MatchingBehaviorType matchingBehavior) {

    Resource matchingResource = matchingBehaviorMap.get(matchingBehavior);
    Resource need = getNeedNode(NeedGraphType.NEED);
    need.removeAll(WON.HAS_MATCHING_BEHAVIOR);
    need.addProperty(WON.HAS_MATCHING_BEHAVIOR, matchingResource);
  }

  public MatchingBehaviorType getMatchingBehavior() {

    RDFNode matchingBehavior = RdfUtils.findOnePropertyFromResource(
      needModel, getNeedNode(NeedGraphType.NEED), WON.HAS_MATCHING_BEHAVIOR);

    if (matchingBehavior == null) {
      // default matching behavior is MUTUAL
      return MatchingBehaviorType.MUTUAL;
    }

    return matchingBehaviorMap.inverse().get(matchingBehavior.asResource());
  }

  public void addFlag(Resource flag) {
    getNeedNode(NeedGraphType.NEED).addProperty(WON.HAS_FLAG, flag);
  }

  public boolean hasFlag(Resource flag) {
    return getNeedNode(NeedGraphType.NEED).hasProperty(WON.HAS_FLAG, flag);
  }

  public void addFacetUri(String facetUri) {

    Resource facet = needModel.createResource(facetUri);
    getNeedNode(NeedGraphType.NEED).addProperty(WON.HAS_FACET, facet);
  }

  public Collection<String> getFacetUris() {

    Collection<String> facetUris = new LinkedList<>();
    NodeIterator iter = needModel.listObjectsOfProperty(getNeedNode(NeedGraphType.NEED), WON.HAS_FACET);
    while (iter.hasNext()) {
      facetUris.add(iter.next().asResource().getURI());
    }
    return facetUris;
  }

  public void setNeedState(NeedState state) {

    Resource stateRes = NeedState.ACTIVE.equals(state) ? WON.NEED_STATE_ACTIVE : WON.NEED_STATE_INACTIVE;
    Resource need = getNeedNode(NeedGraphType.SYSINFO);
    need.removeAll(WON.IS_IN_STATE);
    need.addProperty(WON.IS_IN_STATE, stateRes);
  }

  public NeedState getNeedState() {

    RDFNode state = RdfUtils.findOnePropertyFromResource(sysInfoModel, getNeedNode(NeedGraphType.SYSINFO), WON.IS_IN_STATE);
    if (state.equals(WON.NEED_STATE_ACTIVE)) {
      return NeedState.ACTIVE;
    } else {
      return NeedState.INACTIVE;
    }
  }

  public ZonedDateTime getCreationDate() {

    String dateString = RdfUtils.findOnePropertyFromResource(
      sysInfoModel, getNeedNode(NeedGraphType.SYSINFO), DCTerms.created).asLiteral().getString();
    return ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
  }

  public void setConnectionContainerUri(String containerUri) {
    Resource container = sysInfoModel.createResource(containerUri);
    Resource need = getNeedNode(NeedGraphType.SYSINFO);
    need.removeAll(WON.HAS_CONNECTIONS);
    need.addProperty(WON.HAS_CONNECTIONS, container);
  }

  public String getConnectionContainerUri() {
      return RdfUtils.findOnePropertyFromResource(
        sysInfoModel, getNeedNode(NeedGraphType.SYSINFO), WON.HAS_CONNECTIONS).asResource().getURI();
  }

  public void setWonNodeUri(String nodeUri) {

    Resource node = sysInfoModel.createResource(nodeUri);
    Resource need = getNeedNode(NeedGraphType.SYSINFO);
    need.removeAll(WON.HAS_WON_NODE);
    need.addProperty(WON.HAS_WON_NODE, node);
  }

  public String getWonNodeUri() {
    return RdfUtils.findOnePropertyFromResource(
      sysInfoModel, getNeedNode(NeedGraphType.SYSINFO), WON.HAS_WON_NODE).asResource().getURI();
  }

  public Collection<Resource> getAllContentNodes() {
    return needModel.listResourcesWithProperty(RDF.type, WON.NEED_CONTENT).toSet();
  }

  public Collection<Resource> getContentNodes(NeedContentPropertyType type) {

    Collection<Resource> contentNodes = new LinkedList<>();
    String queryPropertyPath1 = null;
    String queryPropertyPath2 = null;

    switch (type) {
      case IS:
        queryPropertyPath1 = "won:is";
        queryPropertyPath2 = queryPropertyPath1;
        break;
      case SEEKS:
        queryPropertyPath1 = "won:seeks";
        queryPropertyPath2 = queryPropertyPath1;
        break;
      case IS_AND_SEEKS:
        queryPropertyPath1 = "won:is";
        queryPropertyPath2 = "won:seeks";
        break;
      case SEEKS_SEEKS:
        queryPropertyPath1 = "won:seeks/won:seeks";
        queryPropertyPath2 = queryPropertyPath1;
        break;
      case ALL:
        return getAllContentNodes();
    }

    String queryString = "prefix won: <http://purl.org/webofneeds/model#> \n" +
      "SELECT DISTINCT ?contentNode WHERE { \n" +
      "?needNode a won:Need. \n" +
      "?contentNode a won:NeedContent. \n" +
      "?needNode " + queryPropertyPath1 + " ?contentNode. \n " +
      "?needNode " + queryPropertyPath2 + " ?contentNode. }";

    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.create(query, needModel);
    ResultSet rs = qexec.execSelect();

    while(rs.hasNext()) {
      QuerySolution qs = rs.next();
      if (qs.contains("contentNode")) {
        contentNodes.add(qs.get("contentNode").asResource());
      }
    }

    return contentNodes;
  }

  public void setContentPropertyStringValue(NeedContentPropertyType type, Property p, String value) {

    Collection<Resource> nodes = getContentNodes(type);
    for (Resource node : nodes) {
      node.removeAll(p);
      node.addLiteral(p, value);
    }
  }

  public void addContentPropertyStringValue(NeedContentPropertyType type, Property p, String value) {

    Collection<Resource> nodes = getContentNodes(type);
    for (Resource node : nodes) {
      node.addLiteral(p, value);
    }
  }

  public String getContentPropertyStringValue(NeedContentPropertyType type, Property p) {
    return getContentPropertyObject(type, p).asLiteral().getString();
  }

  public String getContentPropertyStringValue(NeedContentPropertyType type, String propertyPath) {
  return getContentPropertyObject(type, propertyPath).getLiteralLexicalForm();
}

  public Collection<String> getContentPropertyStringValues(NeedContentPropertyType type, Property p) {

    Collection<String> values = new LinkedList<>();
    Collection<Resource> nodes = getContentNodes(type);
    for(Resource node : nodes) {
      NodeIterator nodeIterator = needModel.listObjectsOfProperty(node, p);
      while (nodeIterator.hasNext()) {
        values.add(nodeIterator.next().asLiteral().getString());
      }
    }
    return values;
  }

  private RDFNode getContentPropertyObject(NeedContentPropertyType type, Property p) {

    Collection<Resource> nodes = getContentNodes(type);
    RDFNode object = null;
    for(Resource node : nodes) {
      NodeIterator nodeIterator  = needModel.listObjectsOfProperty(node, p);
      if (nodeIterator.hasNext()) {
        if (object != null) {
          throw new IncorrectPropertyCountException("expected exactly one occurrence of property " + p.getURI(), 1, 2);
        }
        object = nodeIterator.next();
      }
    }

    if (object == null) {
      throw new IncorrectPropertyCountException("expected exactly one occurrence of property " + p.getURI(), 1, 0);
    }

    return object;
  }

  private Node getContentPropertyObject(NeedContentPropertyType type, String propertyPath) {

    Path path = PathParser.parse(propertyPath, DefaultPrefixUtils.getDefaultPrefixes());
    Collection<Resource> nodes = getContentNodes(type);

    if (nodes.size() != 1) {
      throw new IncorrectPropertyCountException("expected exactly one occurrence of object for property path " +
                                                  propertyPath, 1, nodes.size());
    }

    URI uri = URI.create(nodes.iterator().next().getURI());
    return RdfUtils.getNodeForPropertyPath(needModel, uri, path);
  }

}
