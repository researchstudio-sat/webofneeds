package won.matcher.solr.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by hfriedrich on 19.07.2016.
 */

public class KneedleTest
{
  @Test
  public void testExampleFromPaper() {

    double[] x = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double[] y = {0, 4, 4.5, 4.66, 4.75, 4.8, 4.83, 4.86, 4.875, 4.88, 4.9 };
    Kneedle kneedle = new Kneedle(1);
    int[] knees = kneedle.detectKneePoints(x,y);
    Assert.assertArrayEquals(new int[]{2}, knees);
  }

  @Test
  public void testDetectMultipleKnees() {

    double[] x = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double[] y = {0.2, 0.2, 0.4, 0.4, 0.4, 0.5, 0.5, 0.5, 0.8, 0.8};
    Kneedle kneedle = new Kneedle(1);
    int[] knees = kneedle.detectKneePoints(x, y);
    Assert.assertArrayEquals(new int[]{2, 5, 8}, knees);
  }

  @Test
  public void testDetectMultipleElbows() {

    double[] x = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double[] y = {0.1, 0.1, 0.3, 0.3, 0.3, 0.6, 0.6, 0.6, 0.9, 0.9};
    Kneedle kneedle = new Kneedle(1);
    int[] knees = kneedle.detectElbowPoints(x, y);
    Assert.assertArrayEquals(new int[]{1, 4, 7}, knees);
  }

}
