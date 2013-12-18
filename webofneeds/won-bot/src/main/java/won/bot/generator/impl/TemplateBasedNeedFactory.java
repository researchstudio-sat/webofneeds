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

package won.bot.generator.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * NeedFactory that reads a need model at startup time and overlays it
 * with the data retrieved from the need factory it wraps.
 */
public class TemplateBasedNeedFactory extends AbstractNeedFactoryWrapper
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Model template;
  private Resource modelResource;

  @Override
  public Model create()
  {
    final Model wrappedModel = getWrappedFactory().create();
    wrappedModel.add(this.template);
    return wrappedModel;
  }

  /**
   * Opens the modelResource and tries to read a jena Model from it.
   */
  public void initialize() {
    logger.info("loading need template model from resource " + this.modelResource);
    this.template = ModelFactory.createDefaultModel();
    Lang lang = RDFLanguages.filenameToLang(this.modelResource.getFilename());
    try {
      RDFDataMgr.read(this.template, modelResource.getInputStream(), lang);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read data from resource " + modelResource);
    }
  }


  public Resource getModelResource()
  {
    return modelResource;
  }

  public void setModelResource(final Resource modelResource)
  {
    this.modelResource = modelResource;
  }
}
