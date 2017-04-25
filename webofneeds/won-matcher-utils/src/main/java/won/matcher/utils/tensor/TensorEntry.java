package won.matcher.utils.tensor;

/**
 * Created by hfriedrich on 21.04.2017.
 */
public class TensorEntry {

    private String sliceName;
    private String needUri;
    private String value;

    public TensorEntry() {
    }

    public TensorEntry(String sliceName, String needUri, String value) {

        setSliceName(sliceName);
        setValue(value);
        setNeedUri(needUri);
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

    public String getNeedUri() {
        return needUri;
    }

    public void setNeedUri(String needUri) {
        this.needUri = needUri;
    }
}
