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
package won.bot.framework.component.atomprosumer;

import won.bot.framework.component.atomconsumer.AtomConsumer;
import won.bot.framework.component.atomproducer.AtomProducer;

/**
 * User: fkleedorfer Date: 18.12.13
 */
public class AtomProsumer {
    private AtomProducer atomProducer;
    private AtomConsumer atomConsumer;

    public void consumeAll() {
        while (!atomProducer.isExhausted() && !atomConsumer.isExhausted()) {
            atomConsumer.consume(atomProducer.create());
        }
    }

    public AtomProducer getAtomProducer() {
        return atomProducer;
    }

    public void setAtomProducer(final AtomProducer atomProducer) {
        this.atomProducer = atomProducer;
    }

    public AtomConsumer getAtomConsumer() {
        return atomConsumer;
    }

    public void setAtomConsumer(final AtomConsumer atomConsumer) {
        this.atomConsumer = atomConsumer;
    }
}
