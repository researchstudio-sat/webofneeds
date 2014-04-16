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

package won.node.rdfstorage.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.node.rdfstorage.RDFStorageService;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.util.RdfUtils;

import java.net.URI;

/**
 * Simple in-memory RDF storage for testing/benchmarking purposes.
 */
public class InMemoryRdfStorageImpl implements RDFStorageService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Dataset dataset = DatasetFactory.createMem();


  @Override
  public void storeContent(final Need need, final Model graph)
  {
    dataset.addNamedModel(need.getNeedURI().toString(), graph);
  }

  @Override
  public Model loadContent(final Need need)
  {
    Model ret = dataset.getNamedModel(need.getNeedURI().toString());
    return ret == null ? null : RdfUtils.cloneModel(ret);
  }

  @Override
  public void storeContent(final ConnectionEvent event, final Model graph)
  {
    dataset.addNamedModel(createEventURI(event), graph);
  }

  @Override
  public Model loadContent(final ConnectionEvent event)
  {
    Model ret = dataset.getNamedModel(createEventURI(event));
    return ret == null ? null : RdfUtils.cloneModel(ret);
  }

  @Override
  public void storeContent(final URI resourceURI, final Model model) {
    dataset.addNamedModel(resourceURI.toString(), model);
  }

  @Override
  public Model loadContent(final URI resourceURI) {
    Model ret = dataset.getNamedModel(resourceURI.toString());
    return ret == null ? null: RdfUtils.cloneModel(ret);
  }

  /**
   * Helper method that creates a URI for the specified ConnectionEvent.
   * TODO: replace by more principled approach for generating event URIs. Do they get a publicly dereferencable URI?
   *
   */
  private String createEventURI(ConnectionEvent event) {
    return event.getConnectionURI().toString()+"/event/" +event.getId();
  }
}
