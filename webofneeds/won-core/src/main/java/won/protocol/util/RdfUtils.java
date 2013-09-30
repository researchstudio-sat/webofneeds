package won.protocol.util;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.util.ResourceUtils;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * User: gabriel
 * Date: 20.05.13
 * Time: 20:19
 */
public class RdfUtils
{

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
    Model m = ModelFactory.createDefaultModel();

    if (content != null) {
      StringReader sr = new StringReader(content);
      m.read(sr, null, "TTL");
    }

    return m;
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
  public static void replaceBaseURI(final Model model, final Resource replacement)
  {
    String baseURI = model.getNsPrefixURI("");
    if (baseURI == null) return;
    Resource baseUriResource = model.getResource(baseURI);
    StmtIterator iterator = model.listStatements(baseUriResource, (Property) null, (RDFNode) null);
    while (iterator.hasNext()) {
      Statement origStmt = iterator.next();
      Statement newStmt = new StatementImpl(replacement, origStmt.getPredicate(), origStmt.getObject());
      model.add(newStmt);
      iterator.remove();
    }
    iterator = model.listStatements(null, (Property) null, (RDFNode) baseUriResource);
    while (iterator.hasNext()) {
      Statement origStmt = iterator.next();
      Statement newStmt = new StatementImpl(origStmt.getSubject(), origStmt.getPredicate(), replacement);
      model.add(newStmt);
      iterator.remove();
    }
    model.setNsPrefix("", replacement.getURI());
  }
}
