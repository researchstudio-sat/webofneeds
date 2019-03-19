/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Groups utility functions for validating WoN RDF structures.
 */
public class ValidationUtils {
    public static List<WonSparqlValidator> loadResources(PathMatchingResourcePatternResolver resolver, String dirString)
            throws IOException {
        List validators = new ArrayList<WonSparqlValidator>();
        Resource[] resources = resolver.getResources("classpath:" + dirString + "*.rq");
        for (Resource resource : resources) {
            try {
                String queryString = loadQueryFromResource(resource);
                Query constraint = QueryFactory.create(queryString);
                WonSparqlValidator validator = new WonSparqlValidator(constraint, resource.getFilename());
                validators.add(validator);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Error loading query from resource " + resource.toString() + ": " + e.getMessage(), e);
            }
        }
        return validators;
    }

    private static String loadQueryFromResource(final Resource resource) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }
}
