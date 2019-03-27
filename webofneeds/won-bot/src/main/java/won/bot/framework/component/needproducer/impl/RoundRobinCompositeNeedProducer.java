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
package won.bot.framework.component.needproducer.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import won.bot.framework.component.needproducer.NeedProducer;

/**
 * Composite Needproducer that will cycle through needFactories, returning a
 * different one each time. The order of the factories returned may change
 * (implemented based on a Set not a List).
 */
public class RoundRobinCompositeNeedProducer extends AbstractCompositeNeedProducer {
    private NeedProducer lastFactory = null;

    @Override
    protected synchronized NeedProducer selectActiveNeedFactory() {
        // work on a copy of the set to avoid concurrency problems
        Set<NeedProducer> factories = new HashSet<NeedProducer>();
        factories.addAll(getNeedFactories());
        Iterator<NeedProducer> factoryIterator = factories.iterator();
        if (!factoryIterator.hasNext()) {
            return null;
        }
        if (lastFactory == null) {
            lastFactory = factoryIterator.next();
            return lastFactory;
        } else {
            // iterate until we reach the last used factory
            while (factoryIterator.hasNext() && factoryIterator.next() != lastFactory) {
            }
            // then, if the iterator has more factories
            if (factoryIterator.hasNext()) {
                // choose the next one after the one we returned last
                this.lastFactory = factoryIterator.next();
                return lastFactory;
            } else {
                // else choose take the first factory returned by the iteraor (we were at the
                // end of the iterators)
                this.lastFactory = factories.iterator().next();
                return this.lastFactory;
            }
        }
    }
}
