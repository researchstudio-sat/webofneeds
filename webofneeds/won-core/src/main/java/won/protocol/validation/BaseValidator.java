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
package won.protocol.validation;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Base class for validators. Provides the validate methods.
 */
public abstract class BaseValidator {
    protected Map<String, List<WonSparqlValidator>> dirToValidator = new LinkedHashMap<>();

    public final boolean validate(Dataset input) {
        for (String dir : dirToValidator.keySet()) {
            List<WonSparqlValidator> validators = dirToValidator.get(dir);
            for (WonSparqlValidator validator : validators) {
                if (!validator.validate(input).isValid()) {
                    return false;
                }
            }
        }
        return true;
    }

    public final boolean validate(Dataset input, StringBuilder causePlaceholder) {
        for (String dir : dirToValidator.keySet()) {
            List<WonSparqlValidator> validators = dirToValidator.get(dir);
            for (WonSparqlValidator validator : validators) {
                WonSparqlValidator.ValidationResult result = validator.validate(input);
                if (!result.isValid()) {
                    causePlaceholder.append(dir);
                    causePlaceholder.append(validator.getName());
                    causePlaceholder.append(": ").append(result.getErrorMessage());
                    return false;
                }
            }
        }
        return true;
    }

    protected void loadSparqlValidatorsFromDirectories(String[] dirs) {
        Map<String, List<WonSparqlValidator>> validatorsPerDir = new HashMap<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String dir : dirs) {
            try {
                List validators = ValidationUtils.loadResources(resolver, dir);
                validatorsPerDir.put(dir, Collections.unmodifiableList(validators));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.dirToValidator = Collections.unmodifiableMap(validatorsPerDir);
    }
}
