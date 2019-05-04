package won.matcher.utils.tensor;

/**
 * Represents an entry in the tensor structure build by
 * {@link TensorMatchingData} in a specified slice, for a specified atom the
 * specified (attribute) value can be set. However instead of an attribute value
 * also an atom can be specified e.g. to connect two atoms in the connection
 * slice. Created by hfriedrich on 21.04.2017.
 */
public class TensorEntry {
    private String sliceName;
    private String atomUri;
    private String value;

    public TensorEntry() {
    }

    public TensorEntry(String sliceName, String atomUri, String value) {
        setSliceName(sliceName);
        setValue(value);
        setAtomUri(atomUri);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSliceName() {
        return sliceName;
    }

    public void setSliceName(String sliceName) {
        this.sliceName = sliceName;
    }

    public String getAtomUri() {
        return atomUri;
    }

    public void setAtomUri(String atomUri) {
        this.atomUri = atomUri;
    }
}
