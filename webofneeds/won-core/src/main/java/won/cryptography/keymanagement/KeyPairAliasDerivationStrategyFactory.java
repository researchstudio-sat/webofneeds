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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Created by fkleedorfer on 22.03.2017.
 */
public class KeyPairAliasDerivationStrategyFactory {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String predefinedAlias = null;

    public KeyPairAliasDerivationStrategy create() {
        if (this.predefinedAlias == null || this.predefinedAlias.trim().length() == 0) {
            logger.info("There is no predefined alias specified for the key pairs that will be used for atoms. ");
            logger.info("This causes each atom to be created with a new key pair, which is slow and may not be intended. To set the predefined alias and thus only use one key pair for all atoms, use the property key 'owner.fixedPrivateKeyAlias=your-key-alias'");
            return new AtomUriAsAliasDerivationStrategy();
        } else {
            logger.info("There is a predefined alias specified for the key pair that will be used for atoms. Its literal value is '{}'",
                            this.predefinedAlias);
            logger.info("This is fast, but each atom created will have the same public key, which might be a privacy risk and may not be intended. To remove the predefined alias and thus use a new key pair for each atom, set the property key 'owner.fixedPrivateKeyAlias=' (i.e., set it to an emptyv alue )");
            return new PredefinedAliasDerivationStrategy(this.predefinedAlias);
        }
    }

    public void setPredefinedAlias(String predefinedAlias) {
        this.predefinedAlias = predefinedAlias;
    }
}
