package won.matcher.solr.utils;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by hfriedrich on 19.07.2016.
 * <p>
 * Detect knee points in a curve using the "Kneedle" algorithm as described in
 * the paper "Finding a" Kneedle" in a Haystack: Detecting Knee Points in System
 * Behavior".
 * <p>
 * NOTE: This implementation does not check the concavity of the curve. Also no
 * smoothing of the curve is applied by this method.
 * <p>
 * Kneedle algorithm described in: Satopaa, V., Albrecht, J., Irwin, D., &
 * Raghavan, B. (2011, June). Finding a" Kneedle" in a Haystack: Detecting Knee
 * Points in System Behavior. In 2011 31st International Conference on
 * Distributed Computing Systems Workshops (pp. 166-171). IEEE.
 */
public class Kneedle {
  private double sensitivity = 1.0;

  public Kneedle() {
    sensitivity = 1.0;
  }

  public Kneedle(final double sensitivity) {
    this.sensitivity = sensitivity;
  }

  public int[] detectKneePoints(final double[] x, final double[] y) {
    return detectKneeOrElbowPoints(x, y, false);
  }

  public int[] detectElbowPoints(final double[] x, final double[] y) {
    return detectKneeOrElbowPoints(x, y, true);
  }

  /**
   * Detect all knee points in a curve according to "Kneedle" algorithm.
   * Alternatively this method can detect elbow points instead of knee points.
   *
   * @param x            x-coordintes of curve, must be increasing in value
   * @param y            y-coordinates of curve
   * @param detectElbows if true detects elbow points, if false detects knee
   *                     points
   * @return array of indices of knee (or elbow) points in the curve
   */
  private int[] detectKneeOrElbowPoints(final double[] x, final double[] y, boolean detectElbows) {

    checkConstraints(x, y);

    List<Integer> kneeIndices = new LinkedList<Integer>();
    List<Integer> lmxIndices = new LinkedList<Integer>();
    List<Double> lmxThresholds = new LinkedList<Double>();

    double[] xn = normalize(x);
    double[] yn = normalize(y);

    // compute the y difference values
    double[] yDiff = new double[y.length];
    for (int i = 0; i < y.length; i++) {
      yDiff[i] = yn[i] - xn[i];
    }

    // if we want to detect elbow points instead of knees do not compute local
    // maxima but local minima instead.
    // Therefore we invert the yDiff values which means we actually find local
    // minima instead of maxima in the
    // original yDiff curve
    if (detectElbows) {
      DescriptiveStatistics stats = new DescriptiveStatistics(yDiff);
      for (int i = 0; i < yDiff.length; i++) {
        yDiff[i] = stats.getMax() - yDiff[i];
      }
    }

    // find local maxima, compute threshold values and detect knee points
    boolean detectKneeForLastLmx = false;
    for (int i = 1; i < y.length - 1; i++) {

      // check if the difference values of a point are bigger
      // than for its left and right neighbour => local maximum
      if (yDiff[i] > yDiff[i - 1] && yDiff[i] > yDiff[i + 1]) {

        // local maximum found
        lmxIndices.add(i);

        // compute the threshold value for this local maximum
        // NOTE: As stated in the paper the threshold Tlmx is computed. Since the mean
        // distance of all consecutive
        // x-values summed together for a normalized function is always (1 / (n -1)) we
        // do not have to compute the
        // whole sum here as stated in the paper.
        double tlmx = yDiff[i] - sensitivity / (xn.length - 1);
        lmxThresholds.add(tlmx);

        // try to find out if the current local maximum is a knee point
        detectKneeForLastLmx = true;
      }

      // check for new knee point
      if (detectKneeForLastLmx) {
        if (yDiff[i + 1] < lmxThresholds.get(lmxThresholds.size() - 1)) {

          // knee detected
          kneeIndices.add(lmxIndices.get(lmxIndices.size() - 1));
          detectKneeForLastLmx = false;
        }
      }
    }

    int knees[] = new int[kneeIndices.size()];
    for (int i = 0; i < kneeIndices.size(); i++) {
      knees[i] = kneeIndices.get(i);
    }

    return knees;
  }

  private double[] normalize(final double[] values) {

    double normalized[] = new double[values.length];
    DescriptiveStatistics stats = new DescriptiveStatistics(values);
    for (int i = 0; i < values.length; i++) {
      normalized[i] = (values[i] - stats.getMin()) / (stats.getMax() - stats.getMin());
    }

    return normalized;
  }

  private void checkConstraints(final double[] x, final double[] y) {

    if (x.length != y.length || x.length < 2) {
      throw new IllegalArgumentException("x and y arrays must have size > 1 and the same number of elements");
    }

    for (int i = 0; i < x.length - 1; i++) {
      if (x[i + 1] <= x[i]) {
        throw new IllegalArgumentException("x values must be sorted and increasing");
      }
    }
  }

}
