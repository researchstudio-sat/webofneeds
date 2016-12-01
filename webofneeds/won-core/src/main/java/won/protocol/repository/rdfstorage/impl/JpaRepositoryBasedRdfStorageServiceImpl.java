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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.protocol.model.DataWithEtag;
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


  @Transactional(propagation = Propagation.REQUIRED)
  @Override
  public void storeModel(final URI resourceURI, final Model model) {
    Dataset dataset = DatasetFactory.createMem();
    dataset.setDefaultModel(model);

    storeDataset(resourceURI, dataset);
  }

  @Transactional(propagation = Propagation.REQUIRED)
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

  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  @Override
  public Model loadModel(final URI resourceURI) {
    DatasetHolder datasetHolder = datasetHolderRepository.findOne(resourceURI);
    return datasetHolder == null ? null : datasetHolder.getDataset().getDefaultModel();
  }

  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  @Override
  public DataWithEtag<Model> loadModel(final URI resourceURI, String etag) {
    Long version = Long.valueOf(etag);
    DatasetHolder datasetHolder = datasetHolderRepository.findOneByUriAndVersionNot(resourceURI, version);
    DataWithEtag<Model> dataWithEtag =
      new DataWithEtag<Model>(datasetHolder == null ? null : datasetHolder.getDataset().getDefaultModel(),
                       datasetHolder == null ? etag : Long.toString(datasetHolder.getVersion()),
                       etag);
    return dataWithEtag;
  }


  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  @Override
  public Dataset loadDataset(final URI resourceURI) {
    DatasetHolder datasetHolder = datasetHolderRepository.findOne(resourceURI);
    return datasetHolder == null ? null : datasetHolder.getDataset();
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  @Override
  public DataWithEtag<Dataset> loadDataset(final URI resourceURI, String etag) {
    Long version = etag == null ? -1 : Long.valueOf(etag);
    DatasetHolder datasetHolder = datasetHolderRepository.findOneByUriAndVersionNot(resourceURI, version);
    DataWithEtag<Dataset> dataWithEtag =
      new DataWithEtag<Dataset>(datasetHolder == null ? null : datasetHolder.getDataset(),
                                datasetHolder == null ? etag : Long.toString(datasetHolder.getVersion()),
                                etag);
    return dataWithEtag;
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  @Override
  public boolean removeContent(final URI resourceURI) {
    try{
      datasetHolderRepository.delete(resourceURI);
    }catch (Exception e){
      return false;
    }
    return true;
  }

}
