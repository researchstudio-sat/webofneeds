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

package won.protocol.util.linkeddata;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.rest.LinkedDataRestClient;

import java.net.URI;

/**
 * LinkedDataSource implementation that uses an in-memory jena Dataset for caching.
 */
public class CachingLinkedDataSource implements LinkedDataSource
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  //In-memory dataset for caching linked data.
  private Dataset dataset = DatasetFactory.createMem();

  //client for accessing linked data.
  private LinkedDataRestClient linkedDataClient = new LinkedDataRestClient();

  public Model getModelForResource(URI resource){
    logger.debug("retrieving model for URI {}", resource);

    if (dataset.containsNamedModel(resource.toString())){
      logger.debug("returning cached instance of model for URI {}", resource);
      return dataset.getNamedModel(resource.toString());
    } else {
      logger.debug("model not in cache for URI {}", resource);
      Model model = linkedDataClient.readResourceData(resource);
      logger.debug("fetched model for URI {}", resource);
      dataset.addNamedModel(resource.toString(), model);
      return model;
    }
  }

}
