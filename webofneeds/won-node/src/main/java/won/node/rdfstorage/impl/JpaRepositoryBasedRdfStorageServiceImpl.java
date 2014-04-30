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

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.rdfstorage.RDFStorageService;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.ModelHolder;
import won.protocol.model.Need;
import won.protocol.repository.ModelHolderRepostory;

import java.net.URI;

/**
 * Rdf Storage service that delegates to a JPA repository
 */
public class JpaRepositoryBasedRdfStorageServiceImpl implements RDFStorageService
{

  @Autowired
  private ModelHolderRepostory modelHolderRepostory;

  @Override
  public void storeContent(final Need need, final Model graph) {
    storeContent(need.getNeedURI(), graph);
  }

  @Override
  public Model loadContent(final Need need) {
    return loadContent(need.getNeedURI());
  }

  @Override
  public void storeContent(final ConnectionEvent event, final Model graph) {
    storeContent(createEventURI(event), graph);
  }

  @Override
  public Model loadContent(final ConnectionEvent event) {
    return loadContent(createEventURI(event));
  }

  @Override
  public void storeContent(final URI resourceURI, final Model model) {
    ModelHolder holder = new ModelHolder(resourceURI, model);
    modelHolderRepostory.save(holder);
  }

  @Override
  public Model loadContent(final URI resourceURI) {
    ModelHolder modelHolder = modelHolderRepostory.findOne(resourceURI);
    return modelHolder == null ? null : modelHolder.getModel();
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
