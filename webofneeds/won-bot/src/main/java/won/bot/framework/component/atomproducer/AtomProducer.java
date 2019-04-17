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
package won.bot.framework.component.atomproducer;

import org.apache.jena.query.Dataset;

/**
 * Interface for producing atoms. The atoms produced must be suitable as the
 * 'model' argument to the createAtom protocol method (i.e., don't have a URI,
 * and only contain user-definable data). Note: Implementations must be
 * thread-safe.
 */
public interface AtomProducer {
    /**
     * Returns a new Model that represents the created atom or null if no more atoms
     * can be created.
     * 
     * @return a dataset or null
     */
    public Dataset create();

    /**
     * Indicates that no more atoms will be created by this factory.
     * 
     * @return
     */
    public boolean isExhausted();
}
