package won.matcher.utils.tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class builds up the relations between atoms and attributes. It to builds
 * an internal tensor data structure (RESCAL three-way-tensor). The data
 * structure can be build incrementally and when finished written to file system
 * for further processing by the RESCAL algorithm or evaluation algorithms.
 * User: hfriedrich Date: 17.07.2014
 */
public class TensorMatchingData {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_DIMENSION = 1000000;
    public static final String HEADERS_FILE = "headers.txt";
    public static final String ATOM_INDICES_FILE = "atomIndices.txt";
    public static final String CONNECTION_SLICE_NAME = "connection";
    private ThirdOrderSparseTensor tensor;
    private ArrayList<String> atoms;
    private ArrayList<String> attributes;
    private ArrayList<String> slices;
    private int nextIndex = 0;

    public TensorMatchingData() {
        tensor = new ThirdOrderSparseTensor(MAX_DIMENSION, MAX_DIMENSION);
        atoms = new ArrayList<>();
        attributes = new ArrayList<>();
        slices = new ArrayList<>();
    }

    public void addAtomConnection(String atom1, String atom2, boolean addOnlyIfAtomsExist) {
        checkAttributeOrAtomName(atom1);
        checkAttributeOrAtomName(atom2);
        if (!addOnlyIfAtomsExist || (addOnlyIfAtomsExist && atoms.contains(atom1) && atoms.contains(atom2))) {
            int x1 = addAtom(atom1);
            int x2 = addAtom(atom2);
            int x3 = addSlice(CONNECTION_SLICE_NAME);
            // connections are bidirectional
            tensor.setEntry(1.0d, x1, x2, x3);
            tensor.setEntry(1.0d, x2, x1, x3);
        }
    }

    public void addAtomAttribute(String sliceName, String atomUri, String attributeValue) {
        checkAttributeOrAtomName(atomUri);
        checkAttributeOrAtomName(attributeValue);
        checkSliceName(sliceName, false);
        int x1 = addAtom(atomUri);
        int x2 = addAttribute(attributeValue);
        int x3 = addSlice(sliceName);
        tensor.setEntry(1.0d, x1, x2, x3);
    }

    public void addAtomAttribute(TensorEntry entry) {
        addAtomAttribute(entry.getSliceName(), entry.getAtomUri(), entry.getValue());
    }

    public String getFirstAttributeOfAtom(String atom, String slice) {
        int atomIndex = atoms.indexOf(atom);
        if (atomIndex < 0) {
            return null;
        }
        Iterator<Integer> iter = tensor.getNonZeroIndicesOfRow(atomIndex, slices.indexOf(slice)).iterator();
        if (iter.hasNext()) {
            return attributes.get(iter.next());
        }
        return null;
    }

    public boolean isValidTensor() {
        return (atoms.size() > 0 && attributes.size() > 0 && slices.size() > 0
                        && getSliceIndex(CONNECTION_SLICE_NAME) != -1);
    }

    public int[] getTensorDimensions() {
        return tensor.getDimensions();
    }

    /**
     * remove empty atoms without attributes and their connections by building up a
     * new matching data object and add only non-empty atoms and connections between
     * those
     */
    protected TensorMatchingData removeEmptyAtomsAndConnections() {
        // build up a new tensor
        TensorMatchingData cleanedMatchingData = new TensorMatchingData();
        // add the non-empty atoms
        for (int i = 0; i < atoms.size(); i++) {
            String atom = atoms.get(i);
            if ((atom != null) && atomHasAttributes(i)) {
                cleanedMatchingData.addAtom(atom);
            }
        }
        // add all attributes and connections to non-empty atoms
        for (int i = 0; i < atoms.size(); i++) {
            String atom = atoms.get(i);
            if ((atom != null) && atomHasAttributes(i)) {
                for (int sliceIndex = 0; sliceIndex < slices.size(); sliceIndex++) {
                    for (int attrIndex : tensor.getNonZeroIndicesOfRow(i, sliceIndex)) {
                        if (slices.get(sliceIndex).equals(CONNECTION_SLICE_NAME)) {
                            cleanedMatchingData.addAtomConnection(atom, atoms.get(attrIndex), true);
                        } else {
                            cleanedMatchingData.addAtomAttribute(slices.get(sliceIndex), atom,
                                            attributes.get(attrIndex));
                        }
                    }
                }
            }
        }
        return cleanedMatchingData;
    }

    /**
     * Add an atom to the atom list
     *
     * @param atom
     * @return
     */
    private int addAtom(String atom) {
        if (!atoms.contains(atom)) {
            atoms.add(nextIndex, atom);
            attributes.add(nextIndex, null);
            nextIndex++;
        }
        return atoms.indexOf(atom);
    }

    /**
     * Add an attribute to the attribute list
     *
     * @param attr
     * @return
     */
    private int addAttribute(String attr) {
        if (!attributes.contains(attr)) {
            attributes.add(nextIndex, attr);
            atoms.add(nextIndex, null);
            nextIndex++;
        }
        return attributes.indexOf(attr);
    }

    private int addSlice(String slice) {
        if (!slices.contains(slice)) {
            slices.add(slice);
        }
        return slices.indexOf(slice);
    }

    /**
     * check if names of atoms/attributes are well-formed
     *
     * @param name
     */
    private void checkAttributeOrAtomName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Atom/Attribute is not allowed to be null or empty");
        }
    }

    private void checkSliceName(String name, boolean connectionSlice) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Slice is not allowed to be null or empty");
        }
        if ((connectionSlice && !name.equals(CONNECTION_SLICE_NAME))
                        || (!connectionSlice && name.equals(CONNECTION_SLICE_NAME))) {
            throw new IllegalArgumentException(
                            "Only connection slice is allowed the name: '" + CONNECTION_SLICE_NAME + "' ");
        }
    }

    /**
     * Used for testing
     *
     * @return
     */
    protected ThirdOrderSparseTensor getTensor() {
        return tensor;
    }

    /**
     * After all the atoms, connections and attributes have been added, this method
     * is used before writing the tensor out to disk, to resize it to the right
     * dimensions and remove connections of empty atoms that do not have any
     * attributes.
     *
     * @return
     */
    protected ThirdOrderSparseTensor createFinalTensor() {
        int dim = getAtoms().size() + getAttributes().size();
        tensor.resize(dim, dim);
        return tensor;
    }

    protected int getSliceIndex(String sliceName) {
        return slices.indexOf(sliceName);
    }

    /**
     * check if an atom with a certain index has any attributes
     *
     * @param atomIndex
     * @return
     */
    private boolean atomHasAttributes(int atomIndex) {
        for (int i = 0; i < slices.size(); i++) {
            if (tensor.hasNonZeroEntryInRow(atomIndex, i) && getSliceIndex(CONNECTION_SLICE_NAME) != i) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getAtomHeaders() {
        return (ArrayList<String>) atoms.clone();
    }

    public List<String> getAtoms() {
        ArrayList<String> continuousList = new ArrayList<>();
        for (String atom : atoms) {
            if (atom != null) {
                continuousList.add(atom);
            }
        }
        return continuousList;
    }

    public List<String> getAttributes() {
        ArrayList<String> continuousList = new ArrayList<>();
        for (String attr : attributes) {
            if (attr != null) {
                continuousList.add(attr);
            }
        }
        return continuousList;
    }

    public List<String> getSlices() {
        ArrayList<String> continuousList = new ArrayList<>(slices);
        return continuousList;
    }

    public int getNumberOfConnections() {
        int connectionSlice = getSliceIndex(CONNECTION_SLICE_NAME);
        return (connectionSlice != -1) ? (getTensor().getNonZeroEntries(connectionSlice) / 2) : 0;
    }

    /**
     * Same as {@link #writeCleanedOutputFiles(String)} but removes empty atoms and
     * their connections before writing the tensor
     *
     * @param folder
     * @return cleaned tensor data
     */
    public TensorMatchingData writeCleanedOutputFiles(String folder) throws IOException {
        if (!isValidTensor()) {
            throw new IllegalStateException("Tensor must filled with data before it can be written");
        }
        logger.info("remove empty atoms and connections ...");
        TensorMatchingData cleanedMatchingData = removeEmptyAtomsAndConnections();
        logger.info("Number of atoms before cleaning: " + getAtoms().size());
        logger.info("Number of atoms after cleaning: " + cleanedMatchingData.getAtoms().size());
        logger.info("Number of attributes before cleaning: " + getAttributes().size());
        logger.info("Number of attributes after cleaning: " + cleanedMatchingData.getAttributes().size());
        logger.info("Number of connections before cleaning: " + getNumberOfConnections());
        logger.info("Number of connections after cleaning: " + cleanedMatchingData.getNumberOfConnections());
        cleanedMatchingData.writeOutputFiles(folder);
        return cleanedMatchingData;
    }

    /**
     * Write the tensor out to the file system for further processing. Create the
     * following files: - header.txt file with the atom/attribute names that
     * correspond to the index in the tensor. - {@literal <Slice>.mtx} files for the
     * different slices e.g. connections, atom type, title and other attributes
     *
     * @param folder
     * @throws IOException
     */
    public void writeOutputFiles(String folder) throws IOException {
        File outFolder = new File(folder);
        outFolder.mkdirs();
        if (!outFolder.isDirectory()) {
            return;
        }
        // write the data file
        // remove the atoms without attributes first
        logger.info("create final tensor ...");
        createFinalTensor();
        int dim = tensor.getDimensions()[0];
        if (dim > MAX_DIMENSION) {
            logger.error("Maximum Dimension {} exceeded: {}", MAX_DIMENSION, dim);
            return;
        }
        logger.info("create tensor data in folder: {}", folder);
        for (int sliceIndex = 0; sliceIndex < slices.size(); sliceIndex++) {
            logger.info("- " + slices.get(sliceIndex) + ".mtx");
            tensor.writeSliceToFile(folder + "/" + slices.get(sliceIndex) + ".mtx", sliceIndex);
        }
        // write the headers file
        FileOutputStream fos = new FileOutputStream(new File(folder + "/" + HEADERS_FILE));
        OutputStreamWriter os = new OutputStreamWriter(fos, "UTF-8");
        for (int i = 0; i < nextIndex; i++) {
            String entity = (atoms.get(i) != null) ? atoms.get(i) : attributes.get(i);
            os.append(entity).append("\n");
        }
        os.close();
        // write the atom indices file
        fos = new FileOutputStream(new File(folder + "/" + ATOM_INDICES_FILE));
        os = new OutputStreamWriter(fos, "UTF-8");
        for (int i = 0; i < nextIndex; i++) {
            if (atoms.get(i) != null) {
                os.append(String.valueOf(i)).append("\n");
            }
        }
        os.close();
        logger.info("- atoms: {}", getAtoms().size());
        logger.info("- attributes: {}", getAttributes().size());
        logger.info("- connections: {}", tensor.getNonZeroEntries(slices.indexOf(CONNECTION_SLICE_NAME)) / 2);
        logger.info("- tensor size: {} x {} x " + tensor.getDimensions()[2], tensor.getDimensions()[0],
                        tensor.getDimensions()[1]);
    }
}
