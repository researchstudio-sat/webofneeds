package won.node.rdfstorage;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 15.02.13
 * Time: 11:22
 * To change this template use File | Settings | File Templates.
 */
public interface RDFStorageService
{

  /**
   * Stores a copy of the specified model, iff it contains at least one triple.
   * @param need
   * @param graph
   */
  public void storeContent(Need need, Model graph);

  public Model loadContent(Need need);

  /**
   * Stores a copy of the specified model, iff it contains at least one triple.
   * @param event
   * @param graph
   */
  public void storeContent(ConnectionEvent event, Model graph);

  public Model loadContent(ConnectionEvent event);

  /**
   * Stores a copy of the specified model, iff it contains at least one triple.
   * @param resourceURI
   * @param model
   */
  public void storeContent(URI resourceURI, Model model);

  public Model loadContent(URI resourceURI);
}
