package won.protocol.message;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/**
 * User: ypanchenko
 * Date: 02.06.2015
 */
public class WonSparqlValidator
{

  public static final Var SELECT_VALIDATION_VARIABLE = Var.alloc("check");
  public static final String SELECT_VALIDATION_PASSED_VALUE = "OK";

  private Query constraint;
  private String name = "unknown";

  public WonSparqlValidator(Query constraint) {
    if (!constraint.isAskType() && !constraint.isSelectType()) {
      throw new IllegalArgumentException("Wrong constriant type!");
    }
    this.constraint = constraint;
  }

  public WonSparqlValidator(Query constraint, String name) {
    if (!constraint.isAskType() && !constraint.isSelectType()) {
      throw new IllegalArgumentException("Wrong constriant type!");
    }
    this.constraint = constraint;
    this.name = name;
  }

  public ValidationResult validate(Dataset input) {
    if (constraint.isAskType()) {
      return validateAsk(input);
    } else if (constraint.isSelectType()) {
      return validateSelect(input);
    }
    return new ValidationResult(false,"Invalid constraint: " + constraint.toString());
  }

  private ValidationResult validateSelect(final Dataset input) {
    try (QueryExecution qe = QueryExecutionFactory.create(constraint, input))
    {
      ResultSet result = qe.execSelect();
      while (result.hasNext()) {
        Binding binding = result.nextBinding();
        Node node = binding.get(SELECT_VALIDATION_VARIABLE);
        if (node != null) {
          if (node.isLiteral()) {
            String resultString = node.getLiteralValue().toString();
            if (SELECT_VALIDATION_PASSED_VALUE.equals(resultString)) {
              return new ValidationResult();
            } else {
              return new ValidationResult(false, "SELECT query produced this binding: " + binding
                .toString());
            }
          }
        }
      }
      return new ValidationResult(false, "SELECT query did not produce a binding for required variable 'check'");
    }
  }

  private void printResult(final ResultSet result) {
    System.out.println(ResultSetFormatter.asText(result));
  }

  private ValidationResult validateAsk(final Dataset input) {
    try (QueryExecution qe = QueryExecutionFactory.create(constraint, input)) {
      if (qe.execAsk()){
        return new ValidationResult(false, "ASK query returned true");
      }
      return new ValidationResult();
    }
  }

  public String getName() {
    return name;
  }

  public static class ValidationResult{
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
