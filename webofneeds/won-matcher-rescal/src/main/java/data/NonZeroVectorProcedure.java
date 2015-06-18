package data;

import org.la4j.vector.functor.VectorProcedure;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Used by {@link data.ThirdOrderSparseTensor} to retrieve a collection of non-zero indices of a vector
 *
 * User: hfriedrich
 * Date: 17.06.2015
 */
public class NonZeroVectorProcedure implements VectorProcedure
{
  private List<Integer> nonZeroIndices;

  public NonZeroVectorProcedure() {
    nonZeroIndices = new LinkedList<>();
  }

  @Override
  public void apply(final int i, final double value) {
    nonZeroIndices.add(i);
  }

  public Collection<Integer> getNonZeroIndices() {
    return nonZeroIndices;
  }
}
