package won.protocol.repository.rdfstorage;

import java.net.URI;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import won.protocol.model.DataWithEtag;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 15.02.13 Time: 11:22 To change this template use File | Settings |
 * File Templates.
 */
public interface RDFStorageService {

    /**
     * Stores a copy of the specified model, iff it contains at least one triple. The model is stored as the default
     * model of a dataset.
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
     * Compares the etag to the value derived from the data found in the storage for the specified URI. Loads the model
     * if the values differ, returns null
     * 
     * @param resourceURI
     * @param etag
     * @return
     */
    public DataWithEtag<Model> loadModel(URI resourceURI, String etag);

    /**
     * Loads the dataset with the specified URL
     * 
     * @param resourceURI
     * @return
     */
    public Dataset loadDataset(URI resourceURI);

    /**
     * Compares the etag to the value derived from the data found in the storage for the specified URI. Loads the model
     * if the values differ, returns null
     * 
     * @param resourceURI
     * @param etag
     * @return
     */
    public DataWithEtag<Dataset> loadDataset(URI resourceURI, String etag);

    public boolean removeContent(URI resourceURI);
}
