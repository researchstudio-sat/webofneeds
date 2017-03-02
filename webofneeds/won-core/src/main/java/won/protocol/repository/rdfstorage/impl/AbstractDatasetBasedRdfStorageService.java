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

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.util.RdfUtils;

import java.net.URI;

/**
 * Simple in-memory RDF storage for testing/benchmarking purposes.
 */
@Deprecated
public abstract class AbstractDatasetBasedRdfStorageService implements RDFStorageService
{
  protected final Logger logger = LoggerFactory.getLogger(getClass());


  public AbstractDatasetBasedRdfStorageService() {

  }

  protected abstract Dataset getDataset();



  @Override
  public void storeModel(final URI resourceURI, final Model model) {
    if (model.isEmpty()) return;
    Model copy = RdfUtils.cloneModel(model);
    Dataset dataset = getDataset();
    try {
      dataset.begin(ReadWrite.WRITE);
      dataset.addNamedModel(resourceURI.toString(), copy);
      dataset.commit();
    } finally {
      dataset.end();
    }
  }

  @Override
  public void storeDataset(final URI resourceURI, final Dataset dataset){
    // class is deprecated
  }

  @Override
  public Model loadModel(final URI resourceURI) {
    Dataset dataset = getDataset();
    try {
      dataset.begin(ReadWrite.READ);
      Model ret = dataset.getNamedModel(resourceURI.toString());
      return ret == null ? null: RdfUtils.cloneModel(ret);
    } finally {
      dataset.end();
    }
  }

  @Override
  public Dataset loadDataset(final URI resourceURI) {
    // class is deprecated
    return null;
  }

  @Override
  public boolean removeContent(final URI resourceURI){
    Dataset dataset = getDataset();
    try{
      dataset.begin(ReadWrite.WRITE);
      dataset.removeNamedModel(resourceURI.toString());
      dataset.commit();
      return true;
    } finally {
      dataset.end();
    }
  }


}
