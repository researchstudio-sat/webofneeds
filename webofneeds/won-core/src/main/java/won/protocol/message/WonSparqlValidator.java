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

  public boolean validate(Dataset input) {
    if (constraint.isAskType()) {
      return validateAsk(input);
    } else if (constraint.isSelectType()) {
      return validateSelect(input);
    }
    return false;
  }

  private boolean validateSelect(final Dataset input) {
    QueryExecution qe = QueryExecutionFactory.create(constraint, input);
    ResultSet result = qe.execSelect();
    // for debugging only, uncomment when writing new validation queries
    //printResult((QueryExecutionFactory.create(constraint, input)).execSelect());

    while (result.hasNext()) {
      Binding binding = result.nextBinding();
      Node node = binding.get(SELECT_VALIDATION_VARIABLE);
      if (node != null) {
        if (node.isLiteral()) {
          String resultString = node.getLiteralValue().toString();
          if (SELECT_VALIDATION_PASSED_VALUE.equals(resultString)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void printResult(final ResultSet result) {
    System.out.println(ResultSetFormatter.asText(result));
  }

  private boolean validateAsk(final Dataset input) {
    QueryExecution qe = QueryExecutionFactory.create(constraint, input);
    return !qe.execAsk();
  }

  public String getName() {
    return name;
  }
}
