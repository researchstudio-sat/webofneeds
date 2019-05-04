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
        Assert.assertEquals(data.getAtoms().size(), 0);
    }

    @Test
    public void addAtomConnection() {
        data.addAtomConnection("Atom1", "Atom2", false);
        Assert.assertEquals(data.getAtoms().size(), 2);
        Assert.assertEquals(data.getAttributes().size(), 0);
        Assert.assertTrue(data.getAtoms().contains("Atom1"));
        Assert.assertTrue(data.getAtoms().contains("Atom2"));
        data.addAtomConnection("Atom1", "Atom3", false);
        Assert.assertEquals(data.getAtoms().size(), 3);
        Assert.assertEquals(data.getAttributes().size(), 0);
        Assert.assertTrue(data.getAtoms().contains("Atom1"));
        Assert.assertTrue(data.getAtoms().contains("Atom2"));
        Assert.assertTrue(data.getAtoms().contains("Atom3"));
    }

    @Test
    public void addAtomAttribute() {
        data.addAtomAttribute("title", "Atom1", "Attr1");
        Assert.assertEquals(data.getAtoms().size(), 1);
        Assert.assertEquals(data.getAttributes().size(), 1);
        Assert.assertTrue(data.getAtoms().contains("Atom1"));
        Assert.assertTrue(data.getAttributes().contains("Attr1"));
        data.addAtomAttribute("description", "Atom1", "Attr2");
        Assert.assertEquals(data.getAtoms().size(), 1);
        Assert.assertEquals(data.getAttributes().size(), 2);
        Assert.assertTrue(data.getAtoms().contains("Atom1"));
        Assert.assertTrue(data.getAttributes().contains("Attr1"));
        Assert.assertTrue(data.getAttributes().contains("Attr2"));
        data.addAtomAttribute("title", "Atom2", "Attr1");
        Assert.assertEquals(data.getAtoms().size(), 2);
        Assert.assertEquals(data.getAttributes().size(), 2);
        Assert.assertTrue(data.getAtoms().contains("Atom1"));
        Assert.assertTrue(data.getAtoms().contains("Atom2"));
        Assert.assertTrue(data.getAttributes().contains("Attr1"));
        Assert.assertTrue(data.getAttributes().contains("Attr2"));
    }

    @Test
    public void checkTensor() throws IOException {
        data.addAtomAttribute("atomType", "Atom1", "OFFER");
        data.addAtomAttribute("title", "Atom1", "Couch");
        data.addAtomAttribute("title", "Atom1", "IKEA");
        data.addAtomAttribute("description", "Atom1", "...");
        data.addAtomAttribute("atomType", "Atom2", "WANT");
        data.addAtomAttribute("title", "Atom2", "Leather");
        data.addAtomAttribute("title", "Atom2", "Couch");
        data.addAtomAttribute("description", "Atom2", "IKEA");
        data.addAtomConnection("Atom1", "Atom2", false);
        data.addAtomAttribute("atomType", "Atom3", "WANT");
        data.addAtomConnection("Atom1", "AtomWithoutAttributes", false);
        data.addAtomAttribute("tag", "Atom2", "#couch");
        data.addAtomAttribute("tag", "Atom4", "#sofa");
        data.addAtomConnection("Atom2", "Atom4", false);
        data.addAtomConnection("Atom1", "AtomWithoutAttributes2", false);
        // number of original different name entries in the tensor header => 14
        ThirdOrderSparseTensor tensor = data.createFinalTensor();
        int[] dim = { 14, 14, 5 };
        Assert.assertArrayEquals(dim, tensor.getDimensions());
        Assert.assertEquals(1.0d, tensor.getEntry(0, 1, data.getSliceIndex("atomType")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(0, 2, data.getSliceIndex("title")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(0, 3, data.getSliceIndex("title")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(0, 4, data.getSliceIndex("description")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 6, data.getSliceIndex("atomType")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 7, data.getSliceIndex("title")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 2, data.getSliceIndex("title")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 3, data.getSliceIndex("description")), DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(0, 5, data.getSliceIndex(TensorMatchingData.CONNECTION_SLICE_NAME)),
                        DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(5, 0, data.getSliceIndex(TensorMatchingData.CONNECTION_SLICE_NAME)),
                        DELTA);
        Assert.assertEquals(1.0d, tensor.getEntry(8, 6, data.getSliceIndex("atomType")), DELTA);
        // 1 connection (symmentric entries) => 2 NZ entries
        Assert.assertEquals(8, tensor.getNonZeroEntries(data.getSliceIndex(TensorMatchingData.CONNECTION_SLICE_NAME)));
        // three atoms with types
        Assert.assertEquals(3, tensor.getNonZeroEntries(data.getSliceIndex("atomType")));
        // 4 title, 2 description, 2 tag attributes
        Assert.assertEquals(4, tensor.getNonZeroEntries(data.getSliceIndex("title")));
        Assert.assertEquals(2, tensor.getNonZeroEntries(data.getSliceIndex("description")));
        Assert.assertEquals(2, tensor.getNonZeroEntries(data.getSliceIndex("tag")));
    }

    @Test
    public void checkCleanedTensor() throws IOException {
        data.addAtomAttribute("atomType", "Atom1", "OFFER");
        data.addAtomAttribute("title", "Atom1", "Couch");
        data.addAtomAttribute("title", "Atom1", "IKEA");
        data.addAtomAttribute("description", "Atom1", "...");
        data.addAtomAttribute("atomType", "Atom2", "WANT");
        data.addAtomAttribute("title", "Atom2", "Leather");
        data.addAtomAttribute("title", "Atom2", "Couch");
        data.addAtomAttribute("description", "Atom2", "IKEA");
        data.addAtomConnection("Atom1", "Atom2", false);
        data.addAtomAttribute("atomType", "Atom3", "WANT");
        data.addAtomConnection("Atom1", "AtomWithoutAttributes", false);
        data.addAtomAttribute("tag", "Atom2", "#couch");
        data.addAtomAttribute("tag", "Atom4", "#sofa");
        data.addAtomConnection("Atom2", "Atom4", false);
        data.addAtomConnection("Atom1", "AtomWithoutAttributes2", false);
        // number of original different name entries in the tensor header => 14,
        // by cleaning the tensor the two Atoms "AtomWithoutAttributes" should be
        // removed
        // together with their connections
        data = data.removeEmptyAtomsAndConnections();
        ThirdOrderSparseTensor tensor = data.createFinalTensor();
        int[] dim = { 12, 12, 5 };
        Assert.assertArrayEquals(dim, tensor.getDimensions());
        List<String> atoms = new LinkedList<>();
        atoms.add("Atom1");
        atoms.add("Atom2");
        atoms.add("Atom3");
        atoms.add("Atom4");
        Assert.assertEquals(atoms, data.getAtoms());
    }
}
