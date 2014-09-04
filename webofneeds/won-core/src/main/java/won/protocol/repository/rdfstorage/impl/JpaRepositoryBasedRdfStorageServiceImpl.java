/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.repository.rdfstorage.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.DatasetHolder;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;

import java.net.URI;

/**
 * Rdf Storage service that delegates to a JPA repository
 */
public class JpaRepositoryBasedRdfStorageServiceImpl implements RDFStorageService
{

  @Autowired
  private DatasetHolderRepository datasetHolderRepository;

  @Override
  public void storeModel(final ConnectionEvent event, final Model graph) {
    storeModel(createEventURI(event), graph);
  }

  @Override
  public Model loadModel(final ConnectionEvent event) {
    return loadModel(createEventURI(event));
  }

  @Override
  public void storeModel(final URI resourceURI, final Model model) {
    Dataset dataset = DatasetFactory.createMem();
    dataset.setDefaultModel(model);

    storeDataset(resourceURI, dataset);
  }

  @Override
  public void storeDataset(final URI resourceURI, final Dataset dataset) {
    DatasetHolder datasetHolder = datasetHolderRepository.findOne(resourceURI);
    if (datasetHolder!=null){
      datasetHolder.setDataset(dataset);
    } else{
      datasetHolder = new DatasetHolder(resourceURI, dataset);
    }
    datasetHolderRepository.save(datasetHolder);
  }

  @Override
  public Model loadModel(final URI resourceURI) {
    DatasetHolder datasetHolder = datasetHolderRepository.findOne(resourceURI);
    return datasetHolder == null ? null : datasetHolder.getDataset().getDefaultModel();
  }

  @Override
  public Dataset loadDataset(final URI resourceURI) {
    DatasetHolder datasetHolder = datasetHolderRepository.findOne(resourceURI);
    return datasetHolder == null ? null : datasetHolder.getDataset();
  }

  @Override
  public boolean removeContent(final URI resourceURI) {
    try{
      datasetHolderRepository.delete(resourceURI);
    }catch (Exception e){
      return false;
    }
    return true;
  }

  /**
   * Helper method that creates a URI for the specified ConnectionEvent.
   * TODO: replace by more principled approach for generating event URIs. Do they get a publicly dereferencable URI?
   *
   */
  private URI createEventURI(ConnectionEvent event) {
    return URI.create(event.getConnectionURI().toString() + "/event/" + event.getId());
  }
}
