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
package won.cryptography.keymanagement;

/**
 * Encapsulates the derivation of an alias for a given Atom URI.
 */
public interface KeyPairAliasDerivationStrategy {
    /**
     * Returns an Alias usable in a KeyStore.
     * 
     * @param atomURI
     * @return always a valid alias string
     * @throws IllegalArgumentException if the argument cannot be converted to an
     * alias.
     */
    String getAliasForAtomUri(String atomURI);
}
