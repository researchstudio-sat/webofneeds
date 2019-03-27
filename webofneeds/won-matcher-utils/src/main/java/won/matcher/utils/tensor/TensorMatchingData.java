package won.matcher.utils.tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class builds up the relations between needs and attributes. It to builds
 * an internal tensor data structure (RESCAL three-way-tensor). The data
 * structure can be build incrementally and when finished written to file system
 * for further processing by the RESCAL algorithm or evaluation algorithms.
 * User: hfriedrich Date: 17.07.2014
 */
public class TensorMatchingData {
    private static final Logger logger = LoggerFactory.getLogger(TensorMatchingData.class);
    private static final int MAX_DIMENSION = 1000000;
    public static final String HEADERS_FILE = "headers.txt";
    public static final String NEED_INDICES_FILE = "needIndices.txt";
    public static final String CONNECTION_SLICE_NAME = "connection";
    private ThirdOrderSparseTensor tensor;
    private ArrayList<String> needs;
    private ArrayList<String> attributes;
    private ArrayList<String> slices;
    private int nextIndex = 0;

    public TensorMatchingData() {
        tensor = new ThirdOrderSparseTensor(MAX_DIMENSION, MAX_DIMENSION);
        needs = new ArrayList<>();
        attributes = new ArrayList<>();
        slices = new ArrayList<>();
    }

    public void addNeedConnection(String need1, String need2, boolean addOnlyIfNeedsExist) {
        checkAttributeOrNeedName(need1);
        checkAttributeOrNeedName(need2);
        if (!addOnlyIfNeedsExist || (addOnlyIfNeedsExist && needs.contains(need1) && needs.contains(need2))) {
            int x1 = addNeed(need1);
            int x2 = addNeed(need2);
            int x3 = addSlice(CONNECTION_SLICE_NAME);
            // connections are bidirectional
            tensor.setEntry(1.0d, x1, x2, x3);
            tensor.setEntry(1.0d, x2, x1, x3);
        }
    }

    public void addNeedAttribute(String sliceName, String needUri, String attributeValue) {
        checkAttributeOrNeedName(needUri);
        checkAttributeOrNeedName(attributeValue);
        checkSliceName(sliceName, false);
        int x1 = addNeed(needUri);
        int x2 = addAttribute(attributeValue);
        int x3 = addSlice(sliceName);
        tensor.setEntry(1.0d, x1, x2, x3);
    }

    public void addNeedAttribute(TensorEntry entry) {
        addNeedAttribute(entry.getSliceName(), entry.getNeedUri(), entry.getValue());
    }

    public String getFirstAttributeOfNeed(String need, String slice) {
        int needIndex = needs.indexOf(need);
        if (needIndex < 0) {
            return null;
        }
        Iterator<Integer> iter = tensor.getNonZeroIndicesOfRow(needIndex, slices.indexOf(slice)).iterator();
        if (iter.hasNext()) {
            return attributes.get(iter.next());
        }
        return null;
    }

    public boolean isValidTensor() {
        return (needs.size() > 0 && attributes.size() > 0 && slices.size() > 0
                        && getSliceIndex(CONNECTION_SLICE_NAME) != -1);
    }

    public int[] getTensorDimensions() {
        return tensor.getDimensions();
    }

    /**
     * remove empty needs without attributes and their connections by building up a
     * new matching data object and add only non-empty needs and connections between
     * those
     */
    protected TensorMatchingData removeEmptyNeedsAndConnections() {
        // build up a new tensor
        TensorMatchingData cleanedMatchingData = new TensorMatchingData();
        // add the non-empty needs
        for (int i = 0; i < needs.size(); i++) {
            String need = needs.get(i);
            if ((need != null) && needHasAttributes(i)) {
                cleanedMatchingData.addNeed(need);
            }
        }
        // add all attributes and connections to non-empty needs
        for (int i = 0; i < needs.size(); i++) {
            String need = needs.get(i);
            if ((need != null) && needHasAttributes(i)) {
                for (int sliceIndex = 0; sliceIndex < slices.size(); sliceIndex++) {
                    for (int attrIndex : tensor.getNonZeroIndicesOfRow(i, sliceIndex)) {
                        if (slices.get(sliceIndex).equals(CONNECTION_SLICE_NAME)) {
                            cleanedMatchingData.addNeedConnection(need, needs.get(attrIndex), true);
                        } else {
                            cleanedMatchingData.addNeedAttribute(slices.get(sliceIndex), need,
                                            attributes.get(attrIndex));
                        }
                    }
                }
            }
        }
        return cleanedMatchingData;
    }

    /**
     * Add a need to the need list
     *
     * @param need
     * @return
     */
    private int addNeed(String need) {
        if (!needs.contains(need)) {
            needs.add(nextIndex, need);
            attributes.add(nextIndex, null);
            nextIndex++;
        }
        return needs.indexOf(need);
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
            needs.add(nextIndex, null);
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
     * check if names of needs/attributes are well-formed
     *
     * @param name
     */
    private void checkAttributeOrNeedName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Need/Attribute is not allowed to be null or empty");
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
     * After all the needs, connections and attributes have been added, this method
     * is used before writing the tensor out to disk, to resize it to the right
     * dimensions and remove connections of empty needs that do not have any
     * attributes.
     *
     * @return
     */
    protected ThirdOrderSparseTensor createFinalTensor() {
        int dim = getNeeds().size() + getAttributes().size();
        tensor.resize(dim, dim);
        return tensor;
    }

    protected int getSliceIndex(String sliceName) {
        return slices.indexOf(sliceName);
    }

    /**
     * check if a need with a certain index has any attributes
     *
     * @param needIndex
     * @return
     */
    private boolean needHasAttributes(int needIndex) {
        for (int i = 0; i < slices.size(); i++) {
            if (tensor.hasNonZeroEntryInRow(needIndex, i) && getSliceIndex(CONNECTION_SLICE_NAME) != i) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getNeedHeaders() {
        return (ArrayList<String>) needs.clone();
    }

    public List<String> getNeeds() {
        ArrayList<String> continuousList = new ArrayList<String>();
        for (String need : needs) {
            if (need != null) {
                continuousList.add(need);
            }
        }
        return continuousList;
    }

    public List<String> getAttributes() {
        ArrayList<String> continuousList = new ArrayList<String>();
        for (String attr : attributes) {
            if (attr != null) {
                continuousList.add(attr);
            }
        }
        return continuousList;
    }

    public List<String> getSlices() {
        ArrayList<String> continuousList = new ArrayList<String>();
        continuousList.addAll(slices);
        return continuousList;
    }

    public int getNumberOfConnections() {
        int connectionSlice = getSliceIndex(CONNECTION_SLICE_NAME);
        return (connectionSlice != -1) ? (getTensor().getNonZeroEntries(connectionSlice) / 2) : 0;
    }

    /**
     * Same as {@link #writeCleanedOutputFiles(String)} but removes empty needs and
     * their connections before writing the tensor
     *
     * @param folder
     * @return cleaned tensor data
     * @throws Exception
     */
    public TensorMatchingData writeCleanedOutputFiles(String folder) throws IOException {
        if (!isValidTensor()) {
            throw new IllegalStateException("Tensor must filled with data before it can be written");
        }
        logger.info("remove empty needs and connections ...");
        TensorMatchingData cleanedMatchingData = removeEmptyNeedsAndConnections();
        logger.info("Number of needs before cleaning: " + getNeeds().size());
        logger.info("Number of needs after cleaning: " + cleanedMatchingData.getNeeds().size());
        logger.info("Number of attributes before cleaning: " + getAttributes().size());
        logger.info("Number of attributes after cleaning: " + cleanedMatchingData.getAttributes().size());
        logger.info("Number of connections before cleaning: " + getNumberOfConnections());
        logger.info("Number of connections after cleaning: " + cleanedMatchingData.getNumberOfConnections());
        cleanedMatchingData.writeOutputFiles(folder);
        return cleanedMatchingData;
    }

    /**
     * Write the tensor out to the file system for further processing. Create the
     * following files: - header.txt file with the need/attribute names that
     * correspond to the index in the tensor. - <Slice>.mtx files for the different
     * slices e.g. connections, need type, title and other attributes
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
        // remove the needs without attributes first
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
            String entity = (needs.get(i) != null) ? needs.get(i) : attributes.get(i);
            os.append(entity + "\n");
        }
        os.close();
        // write the need indices file
        fos = new FileOutputStream(new File(folder + "/" + NEED_INDICES_FILE));
        os = new OutputStreamWriter(fos, "UTF-8");
        for (int i = 0; i < nextIndex; i++) {
            if (needs.get(i) != null) {
                os.append(i + "\n");
            }
        }
        os.close();
        logger.info("- needs: {}", getNeeds().size());
        logger.info("- attributes: {}", getAttributes().size());
        logger.info("- connections: {}", tensor.getNonZeroEntries(slices.indexOf(CONNECTION_SLICE_NAME)) / 2);
        logger.info("- tensor size: {} x {} x " + tensor.getDimensions()[2], tensor.getDimensions()[0],
                        tensor.getDimensions()[1]);
    }
}
