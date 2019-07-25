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

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.component.atomproducer.AtomProducer;

/**
 * AtomProducer that atomFactories to a list of atom factories. Order of
 * delegate request is not guaranteed. Not thread safe.
 */
public abstract class AbstractCompositeAtomProducer implements AtomProducer {
    private Set<AtomProducer> atomFactories = new HashSet<AtomProducer>();
    private static final Logger logger = LoggerFactory.getLogger(AbstractCompositeAtomProducer.class);

    @Override
    public synchronized Dataset create() {
        logger.debug("starting to produce an atom model");
        AtomProducer delegate = selectNonExhaustedAtomFactory();
        if (delegate == null) {
            logger.warn("cannot produce an atom model - all atom factories are exhausted");
            return null; // we're exhausted
        }
        return delegate.create();
    }

    @Override
    public synchronized boolean isExhausted() {
        for (AtomProducer delegate : this.atomFactories) {
            if (!delegate.isExhausted())
                return false;
        }
        return true;
    }

    private AtomProducer selectNonExhaustedAtomFactory() {
        AtomProducer delegate = null;
        // keep fetching delegates, and remove them from the list if they are exhausted
        while ((delegate = selectActiveAtomFactory()) != null && delegate.isExhausted()) {
            // here we have a non-null delegate that is exhausted. Remove it
            this.atomFactories.remove(delegate);
            delegate = null;
        }
        // here, a non-null delegate will not be exhausted. If it is null, we're
        // completely exhausted
        return delegate;
    }

    /**
     * Returns one of the AtomProducer objects found in the atomFactories set, or
     * null if no factories are elegible (in which case the factory is exhausted).
     * 
     * @return
     */
    protected abstract AtomProducer selectActiveAtomFactory();

    protected Set<AtomProducer> getAtomFactories() {
        return atomFactories;
    }

    public void setAtomFactories(final Set<AtomProducer> delegates) {
        this.atomFactories = delegates;
    }

    public void addAtomFactory(AtomProducer atomProducer) {
        this.atomFactories.add(atomProducer);
    }
}
