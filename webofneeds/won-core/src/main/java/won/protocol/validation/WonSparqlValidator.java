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

import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.tdb.TDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * User: ypanchenko
 * Date: 02.06.2015
 */
public class WonSparqlValidator {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static final Var SELECT_VALIDATION_VARIABLE = Var.alloc("check");
    public static final String SELECT_VALIDATION_PASSED_VALUE = "OK";

    private Query constraint;
    private String name = "unknown";

    public WonSparqlValidator(Query constraint) {
        if (!constraint.isAskType() && !constraint.isSelectType()) {
            throw new IllegalArgumentException("Wrong constraint type!");
        }
        this.constraint = constraint;
    }

    public WonSparqlValidator(Query constraint, String name) {
        if (!constraint.isAskType() && !constraint.isSelectType()) {
            throw new IllegalArgumentException("Wrong constraint type!");
        }
        this.constraint = constraint;
        this.name = name;
    }

    public ValidationResult validate(Dataset input) {
        if (logger.isDebugEnabled()) {
            logger.debug("validating constraint of WonSparqlValidator '{}'", name);
        }
        try {
            input.begin(ReadWrite.READ);
            if (constraint.isAskType()) {
                return validateAsk(input);
            } else if (constraint.isSelectType()) {
                return validateSelect(input);
            }
        } finally {
            input.end();
        }
        return new ValidationResult(false, "Invalid constraint: " + constraint.toString());
    }


    private ValidationResult validateSelect(final Dataset input) {
        try (QueryExecution qe = QueryExecutionFactory.create(constraint, input)) {
            qe.getContext().set(TDB.symUnionDefaultGraph, true);
            ResultSet result = qe.execSelect();
            if (!result.hasNext()) {
                //this is a valid result if the projection vars don't contain 'check' (in which case we want exactly one result, see below)
                if (constraint.getProjectVars().stream().noneMatch(var -> "check".equals(var.getVarName()))) {
                    //no 'check' variable: we have a valid result.
                    return new ValidationResult();
                }
            }
            while (result.hasNext()) {
                Binding binding = result.nextBinding();
                Node node = binding.get(SELECT_VALIDATION_VARIABLE);
                if (node != null) {
                    //there is a binding for a variable with name 'check': check its value:
                    if (node.isLiteral()) {
                        String resultString = node.getLiteralValue().toString();
                        if (SELECT_VALIDATION_PASSED_VALUE.equals(resultString)) {
                            return new ValidationResult();
                        } else {
                            return new ValidationResult(false, "SELECT query produced this binding: " + binding
                                    .toString());
                        }
                    }
                } else {
                    // there is no binding for a variable with name 'check':
                    // in this case, we do it similar to checking with ASK: if there are solutions,
                    // they reveal violations of the validity checks
                    // ...
                    // in order to keep results small, we only report the first binding in the ValidationResult
                    String errorMessage = "SPARQL query produced this solution, which indicates a problem: " + binding.toString() + ", query: " + constraint.toString(Syntax.syntaxSPARQL_11);
                    if (result.hasNext()) {
                        //just inform that there are more results
                        errorMessage += ". Note: this is only the first solution. There are more problems.";
                    }
                    return new ValidationResult(false, errorMessage);
                }
                throw new IllegalStateException("We should have returned a result earlier. Bindings: " + binding.toString() + ", Constraint: " + constraint.toString(Syntax.syntaxSPARQL_11));
            }
            throw new IllegalStateException("No result obtained from query, there seems to be some problem with the constraint: " + constraint.toString(Syntax.syntaxSPARQL_11));
        } 
    }

    private void printResult(final ResultSet result) {
        System.out.println(ResultSetFormatter.asText(result));
    }

    private ValidationResult validateAsk(final Dataset input) {
        try (QueryExecution qe = QueryExecutionFactory.create(constraint, input)) {
            if (qe.execAsk()) {
                return new ValidationResult(false, "ASK query returned true");
            }
            return new ValidationResult();
        }
    }

    public String getName() {
        return name;
    }

    public static class ValidationResult {
        private boolean valid = true;
        private String errorMessage = null;

        public ValidationResult() {
        }

        public ValidationResult(final boolean valid, final String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
