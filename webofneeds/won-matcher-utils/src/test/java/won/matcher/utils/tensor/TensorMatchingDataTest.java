package won.matcher.utils.tensor;/*
                                  * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
                                  * the Apache License, Version 2.0 (the "License"); you may not use this file
                                  * except in compliance with the License. You may obtain a copy of the License
                                  * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
                                  * law or agreed to in writing, software distributed under the License is
                                  * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
                                  * KIND, either express or implied. See the License for the specific language
                                  * governing permissions and limitations under the License.
                                  */

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * User: hfriedrich Date: 18.07.2014
 */
public class TensorMatchingDataTest {
    private static final double DELTA = 0.001d;
    private TensorMatchingData data;

    @Before
    public void initData() {
        data = new TensorMatchingData();
    }

    @Test
    public void dataInitialized() {
        Assert.assertEquals(data.getAttributes().size(), 0);
        Assert.assertEquals(data.getNeeds().size(), 0);
    }

    @Test
    public void addNeedConnection() {
        data.addNeedConnection("Need1", "Need2", false);
        Assert.assertEquals(data.getNeeds().size(), 2);
        Assert.assertEquals(data.getAttributes().size(), 0);
        Assert.assertTrue(data.getNeeds().contains("Need1"));
        Assert.assertTrue(data.getNeeds().contains("Need2"));
        data.addNeedConnection("Need1", "Need3", false);
        Assert.assertEquals(data.getNeeds().size(), 3);
        Assert.assertEquals(data.getAttributes().size(), 0);
        Assert.assertTrue(data.getNeeds().contains("Need1"));
        Assert.assertTrue(data.getNeeds().contains("Need2"));
        Assert.assertTrue(data.getNeeds().contains("Need3"));
    }

    @Test
    public void addNeedAttribute() {
        data.addNeedAttribute("title", "Need1", "Attr1");
        Assert.assertEquals(data.getNeeds().size(), 1);
        Assert.assertEquals(data.getAttributes().size(), 1);
        Assert.assertTrue(data.getNeeds().contains("Need1"));
        Assert.assertTrue(data.getAttributes().contains("Attr1"));
        data.addNeedAttribute("description", "Need1", "Attr2");
        Assert.assertEquals(data.getNeeds().size(), 1);
        Assert.assertEquals(data.getAttributes().size(), 2);
        Assert.assertTrue(data.getNeeds().contains("Need1"));
        Assert.assertTrue(data.getAttributes().contains("Attr1"));
        Assert.assertTrue(data.getAttributes().contains("Attr2"));
        data.addNeedAttribute("title", "Need2", "Attr1");
        Assert.assertEquals(data.getNeeds().size(), 2);
        Assert.assertEquals(data.getAttributes().size(), 2);
        Assert.assertTrue(data.getNeeds().contains("Need1"));
        Assert.assertTrue(data.getNeeds().contains("Need2"));
        Assert.assertTrue(data.getAttributes().contains("Attr1"));
        Assert.assertTrue(data.getAttributes().contains("Attr2"));
    }

    @Test
    public void checkTensor() throws IOException {
        data.addNeedAttribute("needType", "Need1", "OFFER");
        data.addNeedAttribute("title", "Need1", "Couch");
        data.addNeedAttribute("title", "Need1", "IKEA");
        data.addNeedAttribute("description", "Need1", "...");
        data.addNeedAttribute("needType", "Need2", "WANT");
        data.addNeedAttribute("title", "Need2", "Leather");
        data.addNeedAttribute("title", "Need2", "Couch");
        data.addNeedAttribute("description", "Need2", "IKEA");
        data.addNeedConnection("Need1", "Need2", false);
        data.addNeedAttribute("needType", "Need3", "WANT");
        data.addNeedConnection("Need1", "NeedWithoutAttributes", false);
        data.addNeedAttribute("tag", "Need2", "#couch");
        data.addNeedAttribute("tag", "Need4", "#sofa");
        data.addNeedConnection("Need2", "Need4", false);
        data.addNeedConnection("Need1", "NeedWithoutAttributes2", false);
        // number of original different name entries in the tensor header => 14
        ThirdOrderSparseTensor tensor = data.createFinalTensor();
        int[] dim = { 14, 14, 5 };
        Assert.assertArrayEquals(dim, tensor.getDimensions());
        Assert.assertEquals(1.0d, tensor.getEntry(0, 1, data.getSliceIndex("needType")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(0, 2, data.getSliceIndex("title")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(0, 3, data.getSliceIndex("title")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(0, 4, data.getSliceIndex("description")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 6, data.getSliceIndex("needType")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 7, data.getSliceIndex("title")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 2, data.getSliceIndex("title")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 3, data.getSliceIndex("description")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(0, 5, data.getSliceIndex(TensorMatchingData.CONNECTION_SLICE_NAME)),
                        DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 0, data.getSliceIndex(TensorMatchingData.CONNECTION_SLICE_NAME)),
                        DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(8, 6, data.getSliceIndex("needType")), DELTA);
        // 1 connection (symmentric entries) => 2 NZ entries
        Assert.assertEquals(8, tensor.getNonZeroEntries(data.getSliceIndex(TensorMatchingData.CONNECTION_SLICE_NAME)));
        // three needs with types
        Assert.assertEquals(3, tensor.getNonZeroEntries(data.getSliceIndex("needType")));
        // 4 title, 2 description, 2 tag attributes
        Assert.assertEquals(4, tensor.getNonZeroEntries(data.getSliceIndex("title")));
        Assert.assertEquals(2, tensor.getNonZeroEntries(data.getSliceIndex("description")));
        Assert.assertEquals(2, tensor.getNonZeroEntries(data.getSliceIndex("tag")));
    }

    @Test
    public void checkCleanedTensor() throws IOException {
        data.addNeedAttribute("needType", "Need1", "OFFER");
        data.addNeedAttribute("title", "Need1", "Couch");
        data.addNeedAttribute("title", "Need1", "IKEA");
        data.addNeedAttribute("description", "Need1", "...");
        data.addNeedAttribute("needType", "Need2", "WANT");
        data.addNeedAttribute("title", "Need2", "Leather");
        data.addNeedAttribute("title", "Need2", "Couch");
        data.addNeedAttribute("description", "Need2", "IKEA");
        data.addNeedConnection("Need1", "Need2", false);
        data.addNeedAttribute("needType", "Need3", "WANT");
        data.addNeedConnection("Need1", "NeedWithoutAttributes", false);
        data.addNeedAttribute("tag", "Need2", "#couch");
        data.addNeedAttribute("tag", "Need4", "#sofa");
        data.addNeedConnection("Need2", "Need4", false);
        data.addNeedConnection("Need1", "NeedWithoutAttributes2", false);
        // number of original different name entries in the tensor header => 14,
        // by cleaning the tensor the two Needs "NeedWithoutAttributes" should be
        // removed
        // together with their connections
        data = data.removeEmptyNeedsAndConnections();
        ThirdOrderSparseTensor tensor = data.createFinalTensor();
        int[] dim = { 12, 12, 5 };
        Assert.assertArrayEquals(dim, tensor.getDimensions());
        List<String> needs = new LinkedList<>();
        needs.add("Need1");
        needs.add("Need2");
        needs.add("Need3");
        needs.add("Need4");
        Assert.assertEquals(needs, data.getNeeds());
    }
}
