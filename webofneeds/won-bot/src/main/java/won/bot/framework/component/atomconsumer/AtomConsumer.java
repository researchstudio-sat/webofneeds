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
package won.bot.framework.component.atomconsumer;

import org.apache.jena.query.Dataset;

/**
 *
 */
public interface AtomConsumer {
    /**
     * Consumes the specified atom object. Implementations must take care not to
     * modify the object as it may be passed to multiple consumers.
     * 
     * @param atom
     */
    public void consume(Dataset atom);

    /**
     * Returns true if the consumer is prepared to consume another atom object.
     * Returns false if not, in which case the consume(..) method may throw an
     * IllegalStateException.
     * 
     * @return
     */
    public boolean isExhausted();
}
