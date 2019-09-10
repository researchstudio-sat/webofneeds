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
package won.bot.framework.component.atomproducer.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import won.protocol.util.RdfUtils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * AtomProducer that reads an atom model at startup time and overlays it with
 * the data retrieved from the atom factory it wraps.
 */
public class TemplateBasedAtomProducer extends AbstractAtomProducerWrapper {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Model templateModel;
    private Resource template;
    private boolean initialized = false;

    @Override
    public synchronized Dataset create() {
        initializeLazily();
        return wrapModel(getWrappedProducer().create());
    }

    private Dataset wrapModel(final Dataset wrappedDataset) {
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
        logger.info("loading atom templateModel model from resource " + this.template);
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
