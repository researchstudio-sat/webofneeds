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
 * Always uses the predefined alias
 */
public class PredefinedAliasStrategy implements KeyPairAliasDerivationStrategy {
    private String predefinedAlias;

    public PredefinedAliasStrategy(String predefinedAlias) {
        this.predefinedAlias = predefinedAlias;
        if (this.predefinedAlias == null || this.predefinedAlias.trim().length() == 0) {
            throw new IllegalArgumentException("Cannot use null or empty string as predefined alias");
        }
    }

    @Override
    public String getAliasForAtomUri(String atomURI) {
        return predefinedAlias;
    }
}
