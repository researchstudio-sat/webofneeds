/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.repository.rdfstorage.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.DataWithEtag;
import won.protocol.model.DatasetHolder;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;

import java.net.URI;
import java.util.Optional;

/**
 * Rdf Storage service that delegates to a JPA repository
 */
public class JpaRepositoryBasedRdfStorageServiceImpl implements RDFStorageService {
    @Autowired
    private DatasetHolderRepository datasetHolderRepository;

    @Override
    public void storeModel(final URI resourceURI, final Model model) {
        Dataset dataset = DatasetFactory.createGeneral();
        dataset.setDefaultModel(model);
        storeDataset(resourceURI, dataset);
    }

    @Override
    public void storeDataset(final URI resourceURI, final Dataset dataset) {
        Optional<DatasetHolder> datasetHolder = datasetHolderRepository.findOneByUri(resourceURI);
        if (datasetHolder.isPresent()) {
            datasetHolder.get().setDataset(dataset);
        } else {
            datasetHolder = Optional.of(new DatasetHolder(resourceURI, dataset));
        }
        datasetHolderRepository.save(datasetHolder.get());
    }

    @Override
    public Model loadModel(final URI resourceURI) {
        Optional<DatasetHolder> datasetHolder = datasetHolderRepository.findOneByUri(resourceURI);
        return datasetHolder.isPresent() ? datasetHolder.get().getDataset().getDefaultModel() : null;
    }

    @Override
    public DataWithEtag<Model> loadModel(final URI resourceURI, String etag) {
        Integer version = Integer.valueOf(etag);
        Optional<DatasetHolder> datasetHolder = datasetHolderRepository.findOneByUriAndVersionNot(resourceURI, version);
        return new DataWithEtag<>(
                        datasetHolder.isPresent() ? datasetHolder.get().getDataset().getDefaultModel() : null,
                        datasetHolder.isPresent() ? Integer.toString(datasetHolder.get().getVersion()) : etag, etag);
    }

    @Override
    public Dataset loadDataset(final URI resourceURI) {
        Optional<DatasetHolder> datasetHolder = datasetHolderRepository.findOneByUri(resourceURI);
        return datasetHolder.isPresent() ? datasetHolder.get().getDataset() : null;
    }

    @Override
    public DataWithEtag<Dataset> loadDataset(final URI resourceURI, String etag) {
        Integer version = etag == null ? -1 : Integer.valueOf(etag);
        Optional<DatasetHolder> datasetHolder = datasetHolderRepository.findOneByUriAndVersionNot(resourceURI, version);
        return new DataWithEtag<>(
                        datasetHolder.isPresent() ? datasetHolder.get().getDataset() : null,
                        datasetHolder.isPresent() ? Integer.toString(datasetHolder.get().getVersion()) : etag, etag);
    }

    @Override
    public boolean removeContent(final URI resourceURI) {
        try {
            datasetHolderRepository.deleteById(resourceURI);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
