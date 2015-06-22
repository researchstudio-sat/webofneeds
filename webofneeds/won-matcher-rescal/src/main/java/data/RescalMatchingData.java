package data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * This class builds up the relations between needs, connections and attributes.
 * It to builds an internal data structure (RESCAL three-way-tensor).
 * The data structure can be build incrementally and when finished written to file system
 * for further processing by the RESCAL algorithm.
 *
 *
 * User: hfriedrich
 * Date: 17.07.2014
 */
public class RescalMatchingData
{
  /**
   * Slices of the RESCAL tensor.
   */
  public enum SliceType
  {
    CONNECTION("connection"),
    NEED_TYPE("needtype"),
    TITLE("subject"),
    DESCRIPTION("content"),
    TAG("tag");
    private String sliceFileName;

    private SliceType(String fileName) {
      sliceFileName = fileName;
    }

    public String getSliceFileName() {
      return sliceFileName;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(RescalMatchingData.class);

  private static final int MAX_DIMENSION = 1000000;
  private static final String NEED_PREFIX = "Need: ";
  private static final String ATTRIBUTE_PREFIX = "Attr: ";
  private static final String HEADERS_FILE = "headers.txt";

  private ThirdOrderSparseTensor tensor;
  private ArrayList<String> needs;
  private ArrayList<String> attributes;
  private int nextIndex = 0;

  public RescalMatchingData() {

    int dim = MAX_DIMENSION;
    tensor = new ThirdOrderSparseTensor(dim, dim, SliceType.values().length, 1);
    needs = new ArrayList<String>();
    attributes = new ArrayList<String>();
  }

  /**
   * Add a connection between two needs if both of these needs already exist (that means have attributes)
   * @param need1
   * @param need2
   */
  public void addNeedConnectionIfNeedsExist(String need1, String need2) {

    if (needs.contains(need1) && needs.contains(need2)) {
      addNeedConnection(need1, need2);
    }
  }

  /**
   * Add a connection between two needs, create the needs if they do not already exist
   * @param need1
   * @param need2
   */
  public void addNeedConnection(String need1, String need2) {

    checkName(need1);
    checkName(need2);
    int x1 = addNeed(need1);
    int x2 = addNeed(need2);
    tensor.setEntry(1.0d, x1, x2, SliceType.CONNECTION.ordinal());
    tensor.setEntry(1.0d, x2, x1, SliceType.CONNECTION.ordinal());
  }

  /**
   * Add the type of a need
   * @param need
   * @param type
   */
  public void addNeedType(String need, String type) {

    checkName(need);
    int x1 = addNeed(need);
    int x2 = addAttribute(type);
    tensor.setEntry(1.0d, x1, x2, SliceType.NEED_TYPE.ordinal());
  }

  /**
   * Add another attribute to a need (e.g. title/description/tag attributes)
   * @param need
   * @param attribute
   * @param attrType
   */
  public void addNeedAttribute(String need, String attribute, SliceType attrType) {

    checkName(need);
    checkName(attribute);
    int x1 = addNeed(need);
    int x2 = addAttribute(attribute);
    int x3 = attrType.ordinal();
    tensor.setEntry(1.0d, x1, x2, x3);
  }

  /**
   * remove empty needs without attributes and their connections by building up a new
   * matching data object and add only non-empty needs and connections between those
   */
  protected RescalMatchingData removeEmptyNeedsAndConnections() {

    // build up a new tensor
    RescalMatchingData cleanedMatchingData = new RescalMatchingData();

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
        for (SliceType slice : SliceType.values()) {
          for (int attrIndex : tensor.getNonZeroIndicesOfRow(i, slice.ordinal())) {
            if (slice.equals(SliceType.CONNECTION)) {
              cleanedMatchingData.addNeedConnectionIfNeedsExist(need, needs.get(attrIndex));
            } else {
              cleanedMatchingData.addNeedAttribute(need, attributes.get(attrIndex), slice);
            }
          }
        }
      }
    }

    return cleanedMatchingData;
  }

  /**
   * Add a need to the need list
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

  /**
   * check if names of needs/attributes are well-formed
   * @param name
   */
  private void checkName(String name) {
    if (name == null || name.equals("")) {
      throw new IllegalArgumentException("Need/Attribute is not allowed to be null or empty");
    }
  }

  /**
   * Used for testing
   * @return
   */
  protected ThirdOrderSparseTensor getTensor() {
    return tensor;
  }

  /**
   * After all the needs, connections and attributes have been added, this method is used
   * before writing the tensor out to disk, to resize it to the right dimensions and
   * remove connections of empty needs that do not have any attributes.
   *
   * @return
   */
  protected ThirdOrderSparseTensor createFinalTensor() {

    logger.info("resize tensor ...");
    int dim = getNeeds().size() + getAttributes().size();
    int maxNZ = 0;
    for (SliceType types : SliceType.values()) {
      maxNZ = Math.max(tensor.getNonZeroEntries(types.ordinal()), maxNZ);
    }
    tensor.resize(dim, dim, SliceType.values().length, maxNZ);
    return tensor;
  }



  /**
   * check if a need with a certain index has any attributes
   * @param needIndex
   * @return
   */
  private boolean needHasAttributes(int needIndex) {

    boolean hasAttribute = (tensor.hasNonZeroEntryInRow(needIndex, SliceType.DESCRIPTION.ordinal()) ||
      tensor.hasNonZeroEntryInRow(needIndex, SliceType.TAG.ordinal()) ||
      tensor.hasNonZeroEntryInRow(needIndex, SliceType.NEED_TYPE.ordinal()) ||
      tensor.hasNonZeroEntryInRow(needIndex, SliceType.TITLE.ordinal()));
    return hasAttribute;
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

  /**
   * Same as {@link #writeCleanedOutputFiles(String)}  but removes empty needs and their connections before writing
   * the tensor
   * @param folder
   * @throws Exception
   */
  public void writeCleanedOutputFiles(String folder) throws IOException {

    int numNeedsBefore = getNeeds().size();
    int numAttributesBefore = getAttributes().size();
    int numConnectionsBefore = getTensor().getNonZeroEntries(SliceType.CONNECTION.ordinal()) / 2;
    logger.info("remove empty needs and connections ...");
    RescalMatchingData cleanedMatchingData = removeEmptyNeedsAndConnections();

    int numConnectionsAfter = cleanedMatchingData.getTensor().getNonZeroEntries(SliceType.CONNECTION.ordinal()) / 2;
    logger.info("Number of needs before cleaning: " + numNeedsBefore);
    logger.info("Number of needs after cleaning: " + cleanedMatchingData.getNeeds().size());
    logger.info("Number of attributes before cleaning: " + numAttributesBefore);
    logger.info("Number of attributes after cleaning: " + cleanedMatchingData.getAttributes().size());
    logger.info("Number of connections before cleaning: " + numConnectionsBefore);
    logger.info("Number of connections after cleaning: " + numConnectionsAfter);
    cleanedMatchingData.writeOutputFiles(folder);
  }

  /**
   * Write the tensor out to the file system for processing by RESCAL.
   * Create the following files:
   * - header.txt file with the need/attribute names that correspond to the index in the tensor.
   * - <Slice>.mtx files for the different slices e.g. connections, need type, title and other attributes
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

    logger.info("create RESCAL data in folder: {}", folder);
    tensor.writeSliceToFile(folder + "/" + SliceType.CONNECTION.getSliceFileName() + ".mtx",
                            SliceType.CONNECTION.ordinal());
    tensor.writeSliceToFile(folder + "/" + SliceType.NEED_TYPE.getSliceFileName() + ".mtx",
                            SliceType.NEED_TYPE.ordinal());
    tensor.writeSliceToFile(folder + "/" + SliceType.TITLE.getSliceFileName() + ".mtx",
                            SliceType.TITLE.ordinal());
    tensor.writeSliceToFile(folder + "/" + SliceType.DESCRIPTION.getSliceFileName() + ".mtx",
                            SliceType.DESCRIPTION.ordinal());
    tensor.writeSliceToFile(folder + "/" + SliceType.TAG.getSliceFileName() + ".mtx",
                            SliceType.TAG.ordinal());

    // write the headers file
    FileOutputStream fos = new FileOutputStream(new File(folder + "/" + HEADERS_FILE));
    OutputStreamWriter os = new OutputStreamWriter(fos, "UTF-8");

    for (int i = 0; i < nextIndex; i++) {
      String entity = (needs.get(i) != null) ? NEED_PREFIX + needs.get(i) : ATTRIBUTE_PREFIX + attributes.get(i);
      os.append(entity + "\n");
    }
    os.close();

    logger.info("- needs: {}", getNeeds().size());
    logger.info("- attributes: {}", getAttributes().size());
    logger.info("- connections: {}", tensor.getNonZeroEntries(SliceType.CONNECTION.ordinal()) / 2);
    logger.info("- tensor size: {} x {} x " + tensor.getDimensions()[2], tensor.getDimensions()[0],
                tensor.getDimensions()[1]);
  }
}
