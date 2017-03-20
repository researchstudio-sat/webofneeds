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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * User: hfriedrich
 * Date: 18.07.2014
 */
public class TensorMatchingDataTest
{
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
  public void addNeedType() {

    data.addNeedType("Need1", "OFFER");
    Assert.assertEquals(data.getNeeds().size(), 1);
    Assert.assertEquals(data.getAttributes().size(), 1);
    Assert.assertTrue(data.getNeeds().contains("Need1"));
    Assert.assertTrue(data.getAttributes().contains("OFFER"));

    data.addNeedType("Need1", "OFFER");
    Assert.assertEquals(data.getNeeds().size(), 1);
    Assert.assertEquals(data.getAttributes().size(), 1);
    Assert.assertTrue(data.getNeeds().contains("Need1"));
    Assert.assertTrue(data.getAttributes().contains("OFFER"));

    data.addNeedType("Need2", "WANT");
    Assert.assertEquals(data.getNeeds().size(), 2);
    Assert.assertEquals(data.getAttributes().size(), 2);
    Assert.assertTrue(data.getNeeds().contains("Need1"));
    Assert.assertTrue(data.getNeeds().contains("Need2"));
    Assert.assertTrue(data.getAttributes().contains("OFFER"));
    Assert.assertTrue(data.getAttributes().contains("WANT"));
  }

  @Test
  public void addNeedConnection() {

    data.addNeedConnection("Need1", "Need2");
    Assert.assertEquals(data.getNeeds().size(), 2);
    Assert.assertEquals(data.getAttributes().size(), 0);
    Assert.assertTrue(data.getNeeds().contains("Need1"));
    Assert.assertTrue(data.getNeeds().contains("Need2"));

    data.addNeedConnection("Need1", "Need3");
    Assert.assertEquals(data.getNeeds().size(), 3);
    Assert.assertEquals(data.getAttributes().size(), 0);
    Assert.assertTrue(data.getNeeds().contains("Need1"));
    Assert.assertTrue(data.getNeeds().contains("Need2"));
    Assert.assertTrue(data.getNeeds().contains("Need3"));
  }

  @Test
  public void addNeedAttribute() {

    data.addNeedAttribute("Need1", "Attr1", TensorMatchingData.SliceType.TITLE);
    Assert.assertEquals(data.getNeeds().size(), 1);
    Assert.assertEquals(data.getAttributes().size(), 1);
    Assert.assertTrue(data.getNeeds().contains("Need1"));
    Assert.assertTrue(data.getAttributes().contains("Attr1"));

    data.addNeedAttribute("Need1", "Attr2", TensorMatchingData.SliceType.DESCRIPTION);
    Assert.assertEquals(data.getNeeds().size(), 1);
    Assert.assertEquals(data.getAttributes().size(), 2);
    Assert.assertTrue(data.getNeeds().contains("Need1"));
    Assert.assertTrue(data.getAttributes().contains("Attr1"));
    Assert.assertTrue(data.getAttributes().contains("Attr2"));

    data.addNeedAttribute("Need2", "Attr1", TensorMatchingData.SliceType.TITLE);
    Assert.assertEquals(data.getNeeds().size(), 2);
    Assert.assertEquals(data.getAttributes().size(), 2);
    Assert.assertTrue(data.getNeeds().contains("Need1"));
    Assert.assertTrue(data.getNeeds().contains("Need2"));
    Assert.assertTrue(data.getAttributes().contains("Attr1"));
    Assert.assertTrue(data.getAttributes().contains("Attr2"));
  }

  @Test
  public void checkTensor() throws IOException {

    data.addNeedType("Need1", "OFFER");
    data.addNeedAttribute("Need1", "Couch", TensorMatchingData.SliceType.TITLE);
    data.addNeedAttribute("Need1", "IKEA", TensorMatchingData.SliceType.TITLE);
    data.addNeedAttribute("Need1", "...", TensorMatchingData.SliceType.DESCRIPTION);
    data.addNeedType("Need2", "WANT");
    data.addNeedAttribute("Need2", "Leather", TensorMatchingData.SliceType.TITLE);
    data.addNeedAttribute("Need2", "Couch", TensorMatchingData.SliceType.TITLE);
    data.addNeedAttribute("Need2", "IKEA", TensorMatchingData.SliceType.DESCRIPTION);
    data.addNeedConnection("Need1", "Need2");
    data.addNeedType("Need3", "WANT");
    data.addNeedConnection("Need1", "NeedWithoutAttributes");
    data.addNeedAttribute("Need2", "#couch", TensorMatchingData.SliceType.TAG);
    data.addNeedAttribute("Need4", "#sofa", TensorMatchingData.SliceType.TAG);
    data.addNeedConnection("Need2", "Need4");
    data.addNeedConnection("Need1", "NeedWithoutAttributes2");

    // number of original different name entries in the tensor header => 14
    ThirdOrderSparseTensor tensor = data.createFinalTensor();
    int[] dim = {14, 14, TensorMatchingData.SliceType.values().length};
    Assert.assertArrayEquals(dim, tensor.getDimensions());

    Assert.assertEquals(1.0d, tensor.getEntry(0, 1, TensorMatchingData.SliceType.NEED_TYPE.ordinal()), DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(0, 2, TensorMatchingData.SliceType.TITLE.ordinal()), DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(0, 3, TensorMatchingData.SliceType.TITLE.ordinal()), DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(0, 4, TensorMatchingData.SliceType.DESCRIPTION.ordinal()),
                        DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(5, 6, TensorMatchingData.SliceType.NEED_TYPE.ordinal()), DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(5, 7, TensorMatchingData.SliceType.TITLE.ordinal()), DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(5, 2, TensorMatchingData.SliceType.TITLE.ordinal()), DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(5, 3, TensorMatchingData.SliceType.DESCRIPTION.ordinal()),
                        DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(0, 5, TensorMatchingData.SliceType.CONNECTION.ordinal()), DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(5, 0, TensorMatchingData.SliceType.CONNECTION.ordinal()), DELTA);
    Assert.assertEquals(1.0d, tensor.getEntry(8, 6, TensorMatchingData.SliceType.NEED_TYPE.ordinal()), DELTA);

    // 1 connection (symmentric entries) => 2 NZ entries
    Assert.assertEquals(8, tensor.getNonZeroEntries(TensorMatchingData.SliceType.CONNECTION.ordinal()));

    // three needs with types
    Assert.assertEquals(3, tensor.getNonZeroEntries(TensorMatchingData.SliceType.NEED_TYPE.ordinal()));

    // 4 title, 2 description, 2 tag attributes
    Assert.assertEquals(4, tensor.getNonZeroEntries(TensorMatchingData.SliceType.TITLE.ordinal()));
    Assert.assertEquals(2, tensor.getNonZeroEntries(TensorMatchingData.SliceType.DESCRIPTION.ordinal()));
    Assert.assertEquals(2, tensor.getNonZeroEntries(TensorMatchingData.SliceType.TAG.ordinal()));
  }

  @Test
  public void checkCleanedTensor() throws IOException {

    data.addNeedType("Need1", "OFFER");
    data.addNeedAttribute("Need1", "Couch", TensorMatchingData.SliceType.TITLE);
    data.addNeedAttribute("Need1", "IKEA", TensorMatchingData.SliceType.TITLE);
    data.addNeedAttribute("Need1", "...", TensorMatchingData.SliceType.DESCRIPTION);
    data.addNeedType("Need2", "WANT");
    data.addNeedAttribute("Need2", "Leather", TensorMatchingData.SliceType.TITLE);
    data.addNeedAttribute("Need2", "Couch", TensorMatchingData.SliceType.TITLE);
    data.addNeedAttribute("Need2", "IKEA", TensorMatchingData.SliceType.DESCRIPTION);
    data.addNeedConnection("Need1", "Need2");
    data.addNeedType("Need3", "WANT");
    data.addNeedConnection("Need1", "NeedWithoutAttributes");
    data.addNeedAttribute("Need2", "#couch", TensorMatchingData.SliceType.TAG);
    data.addNeedAttribute("Need4", "#sofa", TensorMatchingData.SliceType.TAG);
    data.addNeedConnection("Need2", "Need4");
    data.addNeedConnection("Need1", "NeedWithoutAttributes2");

    // number of original different name entries in the tensor header => 14,
    // by cleaning the tensor the two Needs "NeedWithoutAttributes" should be removed
    // together with their connections
    data = data.removeEmptyNeedsAndConnections();
    ThirdOrderSparseTensor tensor = data.createFinalTensor();
    int[] dim = {12, 12, TensorMatchingData.SliceType.values().length};
    Assert.assertArrayEquals(dim, tensor.getDimensions());

    List<String> needs = new LinkedList<>();
    needs.add("Need1");
    needs.add("Need2");
    needs.add("Need3");
    needs.add("Need4");
    Assert.assertEquals(needs, data.getNeeds());
  }

}
