package won.matcher.query.rdf.algebra;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformBase;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.expr.ExprList;

/**
 * Simplifies combinations of bgps and filters. Partly stolen from
 * https://jena.apache.org/documentation/query/manipulating_sparql_using_arq.html
 *
 */
public class QueryCleaner extends TransformBase
{
  @Override
  public Op transform(OpJoin join, Op left, Op right) {

    if (left instanceof OpBGP && right instanceof OpFilter){
      return joinBGPWithFilter(join, (OpBGP)left,(OpFilter)right);
    } else if (left instanceof OpFilter && right instanceof OpBGP) {
      return joinBGPWithFilter(join, (OpBGP)right,(OpFilter)left);
    } else if (left instanceof OpBGP && right instanceof OpBGP){
      return joinBGPs((OpBGP)left, (OpBGP)right);
    } else if (left instanceof OpFilter && right instanceof OpFilter){
      return joinFilters(join, (OpFilter) left, (OpFilter) right);
    }
    else return join;
  }

  private Op joinBGPs(final OpBGP left, final OpBGP right)
  {
    BasicPattern pattern = new BasicPattern(left.getPattern());
    pattern.addAll(right.getPattern());
    return new OpBGP(pattern);
  }

  private Op joinFilters(final OpJoin original, final OpFilter left, final OpFilter right)
  {
    OpFilter leftF = (OpFilter) left;
    OpFilter rightF = (OpFilter) right;

    if (! (leftF.getSubOp() instanceof OpBGP && rightF.getSubOp() instanceof OpBGP) ){
      return original;
    }

    ExprList expressions = new ExprList(leftF.getExprs());
    // Add the RHS filter to the LHS
    expressions.addAll(rightF.getExprs());
    return OpFilter.filter(expressions,joinBGPs((OpBGP)leftF.getSubOp(), (OpBGP)rightF.getSubOp()));
  }

  /**
   * Joins the BGP's contained in the BGP and the filter and returns a filter. If that's not
   * possible because the filters subOp is too complex, the original op is returned.
   * @param original
   * @param opBgp
   * @param opFilter
   * @return
   */
  private Op joinBGPWithFilter(final OpJoin original, final OpBGP opBgp, final OpFilter opFilter)
  {
    if (opFilter.getSubOp() instanceof OpBGP) {
      BasicPattern pattern = new BasicPattern(opBgp.getPattern());
      pattern.addAll(((OpBGP) opFilter.getSubOp()).getPattern());
      return OpFilter.filter(opFilter.getExprs(), new OpBGP(pattern));
    }
    return original;
  }


}