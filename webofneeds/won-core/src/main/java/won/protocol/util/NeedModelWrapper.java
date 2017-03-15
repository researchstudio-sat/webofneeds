package won.protocol.util;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import won.protocol.vocabulary.WON;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by hfriedrich on 16.03.2017.
 */
public class NeedModelWrapper
{
  public enum ContentType {IS, SEEKS, IS_AND_SEEKS, SEEKS_SEEKS};

  private Model model;

  public NeedModelWrapper(String needUri) {

    model = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(model);
    model.createResource(needUri, WON.NEED);
  }

  public NeedModelWrapper(Dataset needDataset) {

    model = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(model);
    RdfUtils.copyDatasetTriplesToModel(needDataset, model);
  }

  public NeedModelWrapper(Model needModel) {
    model = needModel;
  }

  public Model getModel() {
    return model;
  }

  private Resource getNeedNode() {
    return model.listSubjectsWithProperty(RDF.type, WON.NEED).next();
  }

  public Resource createContentNode(ContentType type, String uri) {

    Resource contentNode = (uri != null) ? model.createResource(uri) : model.createResource();
    contentNode.addProperty(RDF.type, WON.NEED_CONTENT);
    Resource needNode = getNeedNode();

    if (ContentType.IS.equals(type)) {
      needNode.addProperty(WON.IS, contentNode);
    } else if (ContentType.SEEKS.equals(type)) {
      needNode.addProperty(WON.SEEKS, contentNode);
    } else if (ContentType.SEEKS_SEEKS.equals(type)) {
      Resource intermediate = model.createResource();
      needNode.addProperty(WON.SEEKS, intermediate);
      intermediate.addProperty(WON.SEEKS, contentNode);
    } else if (ContentType.IS_AND_SEEKS.equals(type)) {
      needNode.addProperty(WON.IS, contentNode);
      needNode.addProperty(WON.SEEKS, contentNode);
    }

    return contentNode;
  }

  public void addFlag(Resource flag) {
    getNeedNode().addProperty(WON.HAS_FLAG, flag);
  }

  public boolean hasFlag(Resource flag) {
    return getNeedNode().hasProperty(WON.HAS_FLAG, flag);
  }

  public void addFacet(String facetUri) {

    Resource facet = model.createResource(facetUri);
    getNeedNode().addProperty(WON.HAS_FACET, facet);
  }

  public void setPropertyStringValue(ContentType type, Property p, String value) {

    Collection<Resource> nodes = getContentNodes(type);
    for (Resource node : nodes) {
      node.removeAll(p);
      node.addLiteral(p, value);
    }
  }

  public void addPropertyStringValue(ContentType type, Property p, String value) {

    Collection<Resource> nodes = getContentNodes(type);
    for (Resource node : nodes) {
      node.addLiteral(p, value);
    }
  }

  public String getPropertyStringValue(ContentType type, Property p) {

    Collection<Resource> nodes = getContentNodes(type);
    for(Resource node : nodes) {
      NodeIterator nodeIterator  = model.listObjectsOfProperty(node, p);
      if (nodeIterator.hasNext()) {
        return nodeIterator.next().asLiteral().getString();
      }
    }
    return null;
  }

  public Collection<String> getPropertyStringValues(ContentType type, Property p) {

    Collection<String> values = new LinkedList<>();
    Collection<Resource> nodes = getContentNodes(type);
    for(Resource node : nodes) {
      NodeIterator nodeIterator = model.listObjectsOfProperty(node, p);
      while (nodeIterator.hasNext()) {
        values.add(nodeIterator.next().asLiteral().getString());
      }
    }
    return values;
  }

  public Collection<Resource> getAllContentNodes() {
    return model.listResourcesWithProperty(RDF.type, WON.NEED_CONTENT).toSet();
  }

  public Collection<Resource> getContentNodes(ContentType type) {

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
    }

    String queryString = "prefix won: <http://purl.org/webofneeds/model#> \n" +
      "SELECT DISTINCT ?contentNode WHERE { \n" +
      "?needNode a won:Need. \n" +
      "?contentNode a won:NeedContent. \n" +
      "?needNode " + queryPropertyPath1 + " ?contentNode. \n " +
      "?needNode " + queryPropertyPath2 + " ?contentNode. }";

    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.create(query, model);
    ResultSet rs = qexec.execSelect();

    while(rs.hasNext()) {
      QuerySolution qs = rs.next();
      if (qs.contains("contentNode")) {
        contentNodes.add(qs.get("contentNode").asResource());
      }
    }

    return contentNodes;
  }

}
