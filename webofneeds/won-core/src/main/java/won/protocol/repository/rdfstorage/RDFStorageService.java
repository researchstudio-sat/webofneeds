package won.protocol.repository.rdfstorage;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.model.ConnectionEvent;

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
   * @param event
   * @param graph
   */
  public void storeModel(ConnectionEvent event, Model graph);

  public Model loadModel(ConnectionEvent event);

  /**
   * Stores a copy of the specified model, iff it contains at least one triple.
   * The model is stored as the default model of a dataset.
   *
   * @param resourceURI
   * @param model
   */
  public void storeModel(URI resourceURI, Model model);

  /**
   * Stores a copy of the specified dataset.
   *
   * @param resourceURI
   * @param dataset
   */
  public void storeDataset(URI resourceURI, Dataset dataset);

  /**
   * Loads the default model of the stored dataset with the specified URL
   *
   * @param resourceURI
   * @return
   */
  public Model loadModel(URI resourceURI);

  /**
   * Loads the dataset with the specified URL
   * @param resourceURI
   * @return
   */
  public Dataset loadDataset(URI resourceURI);

  public boolean removeContent(URI resourceURI);
}
