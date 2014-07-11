package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.NamedGraph;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Prefix;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Triple;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

/**
 * Created by ypanchenko on 09.07.2014.
 */
public class ModelConverter
{


  /**
   *  Converts Signingframework's NamedGraph into Jena's Model.
   *
   *  In the base URI, only <http://www.example.com/resource/need/12#>
   *  is supported.  <http://www.example.com/resource/need/12> or
   *  <http://www.example.com/resource/need/12/> are not supported,
   *  for reasons, see comments in test_1_cupboard.trig
   *
   */
  public static Model namedGraphToModel(String graphName, GraphCollection gc) throws Exception {
    NamedGraph graph = null;
    for (NamedGraph g : gc.getGraphs()) {
      if (g.getName().equals(graphName)) {
        graph = g;
        break;
      }
    }
    return namedGraphToModel(graph, gc.getPrefixes());
  }


  /**
   * Converts Jena's Model into Signingframework's NamedGraph of GraphCollection.
   *
   *  In the base URI, only <http://www.example.com/resource/need/12#>
   *  is supported.  <http://www.example.com/resource/need/12> or
   *  <http://www.example.com/resource/need/12/> are not supported,
   *  for reasons, see comments in test_1_cupboard.trig
   */
  public static GraphCollection modelToGraphCollection(String modelURI, Dataset modelDataset) {

    Map<String, String> pm = modelDataset.getDefaultModel().getNsPrefixMap();
    Model model = modelDataset.getNamedModel(modelURI);

    return modelToGraphCollection(modelURI, model, pm);
  }


  // TODO prefixes generalization (if graph name is provided as URI or as local name)
  // TODO or provide Dataset as the second argument, so that prefixes are extracted inside the method
  private static Model namedGraphToModel(NamedGraph graph, List<Prefix> prefixes) throws Exception {

    // Here, the simplest, but probably not the most efficient, approach is
    // applied:  Signingframework's reader and Jena's writer are used to
    // transform data from one data structure to another. Works fine.

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    String dfPref = "";
    for (Prefix p : prefixes) {
      writer.write(p.toString());
      //TODO chng prefix handling
      if (p.getPrefix().equals(":")) {
        dfPref = p.getIriContent();
      }
    }
    TriGPlusWriter.writeGraph(writer, graph, 3);
    writer.close();
    String content = os.toString();

    ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());

    Model model = dataset.getNamedModel(dfPref + graph.getName().replace(":", ""));
    for (Prefix pref : prefixes) {
      model.setNsPrefix(pref.getPrefix().replace(":", ""), pref.getIriContent());
    }
    // or default graph of dataset also has prefix map

    return model;
  }




  // TODO prefixes generalization (if graph name is provided as URI or as local name)
  private static GraphCollection modelToGraphCollection(String name, Model model, Map<String, String> pm) {

    // Convert each subj pred obj in Jena Statement into String and add to
    // SigningFramework's NamedGraph.
    //The simpler approach with just using Jena's writer and Signingframework's
    //reader to transform data between data structures won't work since
    //Signingframework has problems with recognizing the [] structure

    GraphCollection graphc = new GraphCollection();
    NamedGraph namedGraph = new NamedGraph(enclose(name, "<", ">"), 0, null);
    StmtIterator iterator = model.listStatements();
    while (iterator.hasNext()) {
      Statement stmt = iterator.nextStatement();
      String subjString = rdfNodeAsString(stmt.getSubject());
      String objString = rdfNodeAsString(stmt.getObject());
      String predString = enclose(stmt.getPredicate().asResource().getURI(), "<", ">");
      Triple gcTriple = new Triple(subjString, predString, objString);
      namedGraph.addTriple(gcTriple);
    }
    graphc.addGraph(namedGraph);

    for (String prefix : pm.keySet()) {
      graphc.addPrefix(new Prefix(prefix + ":", "<" + pm.get(prefix) + ">"));
    }
    graphc.applyPrefixes();

    return graphc;
  }


  private static String rdfNodeAsString(final RDFNode rdfNode) {
    String result = null;
    if (rdfNode.isURIResource()) {
      String uri = rdfNode.asResource().getURI();
      //if (uri.endsWith("/")) {
      //  uri = uri.substring(0, uri.length() - 1);
      //}
      result = enclose(uri, "<", ">");
    } else if (rdfNode.isLiteral()) {
      result = enclose(rdfNode.asLiteral().getLexicalForm(), "\"", "\"");
      if (rdfNode.asLiteral().getDatatypeURI() != null) {
        result = enclose(result, "", "^^" + "<" + rdfNode.asLiteral().getDatatypeURI() + ">");
      } else if (rdfNode.asLiteral().getLanguage() != null && !rdfNode.asLiteral().getLanguage().isEmpty()) {
        result = enclose(result, "", "@" + rdfNode.asLiteral().getLanguage());
      }
    } else if (rdfNode.isAnon()) {
      result = enclose(rdfNode.asResource().getId().getLabelString(), "_:", "");
    } else { // It might need to be improved as some syntax cases might not be covered so far
      // a collection??
      throw new UnsupportedOperationException("support missing for converting: " + rdfNode.toString());
    }
    return result;
  }


  private static String enclose(String string, String start, String end) {
    return start + string + end;
  }
}
