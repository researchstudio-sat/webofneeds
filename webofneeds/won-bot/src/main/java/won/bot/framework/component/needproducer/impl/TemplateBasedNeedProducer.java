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

package won.bot.framework.component.needproducer.impl;

import java.io.IOException;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import won.protocol.model.NeedGraphType;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;

/**
 * NeedProducer that reads a need model at startup time and overlays it with the
 * data retrieved from the need factory it wraps.
 */
public class TemplateBasedNeedProducer extends AbstractNeedProducerWrapper {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Model templateModel;
  private Resource template;
  private boolean initialized = false;

  @Override
  public synchronized Dataset create() {
    initializeLazily();
    return wrapModel(getWrappedProducer().create());
  }

  private Dataset wrapModel(final Dataset wrappedDataset) {
    if (this.templateModel != null) {
      // TODO: TEMPLATE BASED PRODUCER IS WEIRD NOW
      NeedModelWrapper needModelWrapper = new NeedModelWrapper(wrappedDataset);
      Model needModel = needModelWrapper.copyNeedModel(NeedGraphType.NEED);
      Model wrappedModel = RdfUtils.mergeModelsCombiningBaseResource(needModel, this.templateModel);
    }
    return wrappedDataset;
  }

  private void initializeLazily() {
    if (!initialized) {
      initialize();
    }
  }

  /**
   * Opens the template and tries to read a jena Model from it.
   */
  public void initialize() {
    if (this.initialized)
      return;
    this.initialized = true;
    loadTemplateModel();
  }

  private void loadTemplateModel() {
    logger.info("loading need templateModel model from resource " + this.template);
    Lang lang = RDFLanguages.filenameToLang(this.template.getFilename());
    try {
      this.templateModel = RdfUtils.readRdfSnippet(template.getInputStream(), Lang.TTL.getLabel());
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read data from resource " + template);
    }
    if (this.templateModel == null) {
      logger.warn(
          "reading RDF data from template {} resulted in a null or empty model. Wrapped models will not be modified",
          this.template);
    }
  }

  public Resource getTemplate() {
    return template;
  }

  public void setTemplate(final Resource template) {
    this.template = template;
  }
}
