package won.matcher.utils.tensor;/*
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

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * User: hfriedrich
 * Date: 09.07.2014
 */
public class ThirdOrderSparseTensorTest {
    private static final double DELTA = 0.001d;

    private ThirdOrderSparseTensor testTensor1;

    @Before
    public void initTestTensor() {
        testTensor1 = new ThirdOrderSparseTensor(4, 4);
    }

    @Test
    public void tensorCreation() {

        ThirdOrderSparseTensor tensor = new ThirdOrderSparseTensor(4, 3);
        int[] dim = {4, 3, 0};
        Assert.assertArrayEquals(dim, tensor.getDimensions());
        for (int x3 = 0; x3 < dim[2]; x3++) {
            for (int x2 = 0; x2 < dim[1]; x2++) {
                for (int x1 = 0; x1 < dim[0]; x1++) {
                    Assert.assertEquals(0.0d, tensor.getEntry(x1, x2, x3), 0.0d);
                }
            }
        }
    }

    @Test
    public void setGetEntry() {

        testTensor1.setEntry(0.5d, 0, 0, 0);
        testTensor1.setEntry(1.0d, 0, 0, 0);
        testTensor1.setEntry(2.0d, 1, 0, 1);
        testTensor1.setEntry(3.0d, 0, 2, 2);
        testTensor1.setEntry(4.0d, 3, 3, 2);

        Assert.assertEquals(1.0d, testTensor1.getEntry(0, 0, 0), DELTA);
        Assert.assertEquals(2.0d, testTensor1.getEntry(1, 0, 1), DELTA);
        Assert.assertEquals(3.0d, testTensor1.getEntry(0, 2, 2), DELTA);
        Assert.assertEquals(4.0d, testTensor1.getEntry(3, 3, 2), DELTA);

        testTensor1.setEntry(0.0d, 3, 3, 2);
        Assert.assertEquals(0.0d, testTensor1.getEntry(3, 3, 2), DELTA);
    }

    @Test
    public void resizeUp() {

        int[] dim = testTensor1.getDimensions();
        testTensor1.setEntry(1.0d, 3, 1, 2);
        int[] newDim = {dim[0] + 1, dim[1] + 2, dim[2] + 3};
        testTensor1.resize(newDim[0], newDim[1]);
        Assert.assertArrayEquals(newDim, testTensor1.getDimensions());
        Assert.assertEquals(1.0d, testTensor1.getEntry(3, 1, 2), DELTA);

        for (int x3 = 0; x3 < newDim[2]; x3++) {
            for (int x2 = 0; x2 < newDim[1]; x2++) {
                for (int x1 = 0; x1 < newDim[0]; x1++) {
                    if (x1 != 3 || x2 != 1 || x3 != 2) {
                        Assert.assertEquals(0.0d, testTensor1.getEntry(x1, x2, x3), 0.0d);
                    }
                }
            }
        }
    }

    @Test
    public void resizeDown() {

        int[] dim = testTensor1.getDimensions();
        testTensor1.setEntry(1.0d, 3, 1, 2);
        int[] newDim = {dim[0] - 1, dim[1] - 1, 3};
        testTensor1.resize(newDim[0], newDim[1]);
        Assert.assertArrayEquals(newDim, testTensor1.getDimensions());

        for (int x3 = 0; x3 < newDim[2]; x3++) {
            for (int x2 = 0; x2 < newDim[1]; x2++) {
                for (int x1 = 0; x1 < newDim[0]; x1++) {
                    Assert.assertEquals(0.0d, testTensor1.getEntry(x1, x2, x3), 0.0d);
                }
            }
        }
    }

    @Test
    public void scaleSlicesDynamically() {

        Assert.assertEquals(0, testTensor1.getDimensions()[2]);
        testTensor1.setEntry(1.0, 1, 1, 0);
        Assert.assertEquals(1, testTensor1.getDimensions()[2]);
        Assert.assertEquals(1.0d, testTensor1.getEntry(1, 1, 0), DELTA);
        Assert.assertEquals(0.0d, testTensor1.getEntry(0, 0, 0), DELTA);
        testTensor1.setEntry(1.0, 2, 3, 0);
        Assert.assertEquals(1, testTensor1.getDimensions()[2]);
        testTensor1.setEntry(1.0, 2, 3, 2);
        Assert.assertEquals(1.0d, testTensor1.getEntry(2, 3, 2), DELTA);
        Assert.assertEquals(3, testTensor1.getDimensions()[2]);
        testTensor1.setEntry(1.0, 2, 3, 1);
        Assert.assertEquals(3, testTensor1.getDimensions()[2]);
        testTensor1.resize(5, 5);
        testTensor1.setEntry(1.0, 2, 3, 3);
        Assert.assertEquals(4, testTensor1.getDimensions()[2]);
    }

    @Test
    public void getNonZeroIndicesOfRow() {

        testTensor1.setEntry(0.5d, 0, 0, 0);
        testTensor1.setEntry(1.0d, 0, 1, 0);
        testTensor1.setEntry(0.5d, 1, 0, 0);
        testTensor1.setEntry(1.0d, 1, 1, 1);

        Collection<Integer> indices = new ArrayList<>();
        indices.add(0);
        indices.add(1);
        Assert.assertEquals(indices, testTensor1.getNonZeroIndicesOfRow(0, 0));

        indices.clear();
        indices.add(0);
        Assert.assertEquals(indices, testTensor1.getNonZeroIndicesOfRow(1, 0));
    }

    @Test
    public void hasNonZeroEntryInRow() {

        testTensor1.setEntry(0.5d, 0, 0, 0);
        testTensor1.setEntry(1.0d, 0, 0, 0);
        testTensor1.setEntry(2.0d, 1, 0, 1);
        testTensor1.setEntry(3.0d, 0, 2, 2);
        testTensor1.setEntry(3.0d, 1, 2, 2);
        testTensor1.setEntry(4.0d, 3, 3, 2);
        testTensor1.setEntry(4.0d, 3, 3, 0);

        Assert.assertTrue(testTensor1.hasNonZeroEntryInRow(0, 0));
        Assert.assertTrue(testTensor1.hasNonZeroEntryInRow(1, 1));
        Assert.assertTrue(testTensor1.hasNonZeroEntryInRow(0, 2));
        Assert.assertTrue(testTensor1.hasNonZeroEntryInRow(1, 2));
        Assert.assertTrue(testTensor1.hasNonZeroEntryInRow(3, 2));
        Assert.assertTrue(testTensor1.hasNonZeroEntryInRow(3, 0));

        Assert.assertFalse(testTensor1.hasNonZeroEntryInRow(0, 1));
        Assert.assertFalse(testTensor1.hasNonZeroEntryInRow(1, 0));
        Assert.assertFalse(testTensor1.hasNonZeroEntryInRow(1, 0));
        Assert.assertFalse(testTensor1.hasNonZeroEntryInRow(2, 0));
        Assert.assertFalse(testTensor1.hasNonZeroEntryInRow(2, 1));
        Assert.assertFalse(testTensor1.hasNonZeroEntryInRow(2, 2));

    }
}
